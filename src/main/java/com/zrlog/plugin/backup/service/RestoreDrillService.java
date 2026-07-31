package com.zrlog.plugin.backup.service;

import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.backup.Application;
import com.zrlog.plugin.backup.model.BackupConfigValues;
import com.zrlog.plugin.backup.model.RestoreDrillResult;
import com.zrlog.plugin.backup.model.WebsiteKeyRequest;
import com.zrlog.plugin.backup.scheduler.BackupJob;
import com.zrlog.plugin.backup.util.AESCrypto;
import com.zrlog.plugin.common.IOUtil;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.message.DbPropertiesResponse;
import com.zrlog.plugin.type.ActionType;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RestoreDrillService {

    private static final List<String> CORE_TABLES = Arrays.asList("log", "user", "website");
    private static final String TEMPORARY_DATABASE_PREFIX = "zrlog_restore_drill_";
    private static final Pattern TEMPORARY_DATABASE_PATTERN = Pattern.compile(
            "^" + TEMPORARY_DATABASE_PREFIX + "(\\d{13})_([0-9a-f]{12})$"
    );
    private static final long STALE_DATABASE_AGE_MILLIS = 24L * 60 * 60 * 1000;
    private final VerificationRecorder verificationRecorder;

    public RestoreDrillService() {
        this(BackupProtectionRepository::recordVerification);
    }

    RestoreDrillService(VerificationRecorder verificationRecorder) {
        this.verificationRecorder = verificationRecorder;
    }

    public RestoreDrillResult run(IOSession session) {
        RestoreDrillResult result = new RestoreDrillResult();
        result.setStartedAt(System.currentTimeMillis());
        File backupFile = null;
        try {
            BackupConfigValues config = session.getResponseSync(
                    ContentType.JSON,
                    WebsiteKeyRequest.of("backupPassword,backupFilePath"),
                    ActionType.GET_WEBSITE,
                    BackupConfigValues.class
            );
            backupFile = latestBackup(resolveBackupPath(config));
            result.setFileName(backupFile.getName());
            result.setFileSha256(sha256(backupFile));
            Properties properties = databaseProperties(session);
            restoreAndVerify(backupFile, config == null ? null : config.getBackupPassword(), properties, result);
            result.setSuccess(true);
            result.setMessage("Restore drill completed successfully");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            if (backupFile != null && result.getFileName() == null) {
                result.setFileName(backupFile.getName());
            }
        } finally {
            result.setCompletedAt(System.currentTimeMillis());
            try {
                verificationRecorder.record(session, result);
            } catch (Exception evidenceError) {
                String evidenceMessage = evidenceError.getMessage() == null
                        ? evidenceError.getClass().getSimpleName() : evidenceError.getMessage();
                String originalMessage = result.getMessage();
                result.setSuccess(false);
                result.setMessage((originalMessage == null || originalMessage.trim().isEmpty() ? "" : originalMessage + "; ")
                        + "Failed to record restore verification evidence: " + evidenceMessage);
            }
        }
        return result;
    }

    private String resolveBackupPath(BackupConfigValues config) {
        if (config == null || config.getBackupFilePath() == null || config.getBackupFilePath().trim().isEmpty()) {
            return Application.sqlPath;
        }
        return config.getBackupFilePath();
    }

    private File latestBackup(String backupPath) {
        File[] files = new File(backupPath).listFiles(
                file -> file.isFile() && BackupJob.isSqlFile(file)
        );
        if (files == null || files.length == 0) {
            throw new IllegalStateException("No database backup is available for restore verification");
        }
        return Arrays.stream(files)
                .max(Comparator.comparingLong(File::lastModified))
                .orElseThrow(() -> new IllegalStateException("No database backup is available"));
    }

    private Properties databaseProperties(IOSession session) throws IOException {
        DbPropertiesResponse response = session.getResponseSync(
                ContentType.JSON, new WebsiteKeyRequest(), ActionType.GET_DB_PROPERTIES, DbPropertiesResponse.class
        );
        if (response == null || response.getDbProperties() == null) {
            throw new IOException("Database configuration is unavailable");
        }
        Properties properties = new Properties();
        try (FileInputStream inputStream = new FileInputStream(response.getDbProperties())) {
            properties.load(inputStream);
        }
        return properties;
    }

    void restoreAndVerify(File backupFile,
                          String backupPassword,
                          Properties properties,
                          RestoreDrillResult result) throws Exception {
        String jdbcUrl = Objects.requireNonNull(properties.getProperty("jdbcUrl"), "jdbcUrl is required");
        String user = Objects.requireNonNull(properties.getProperty("user"), "database user is required");
        String password = properties.getProperty("password", "");
        long now = System.currentTimeMillis();
        String temporaryDatabase = TEMPORARY_DATABASE_PREFIX + now + "_"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection connection = DriverManager.getConnection(serverJdbcUrl(jdbcUrl), user, password)) {
            cleanupStaleDatabases(connection, now);
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE DATABASE `" + temporaryDatabase + "` CHARACTER SET utf8mb4");
                statement.execute("USE `" + temporaryDatabase + "`");
            }
            try {
                try (Reader reader = backupReader(backupFile, backupPassword)) {
                    executeDump(connection, MysqlDumpStatements.read(reader));
                }
                result.setRestoredTableCount(tableCount(connection, temporaryDatabase));
                result.setRestoredCoreRowCount(verifyCoreTables(connection, temporaryDatabase));
            } finally {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("DROP DATABASE IF EXISTS `" + temporaryDatabase + "`");
                }
            }
        }
    }

    private void cleanupStaleDatabases(Connection connection, long now) throws Exception {
        List<String> staleDatabases = new java.util.ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT schema_name FROM information_schema.schemata WHERE schema_name LIKE ?")) {
            statement.setString(1, TEMPORARY_DATABASE_PREFIX + "%");
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String database = resultSet.getString(1);
                    if (isStaleTemporaryDatabase(database, now)) {
                        staleDatabases.add(database);
                    }
                }
            }
        }
        for (String database : staleDatabases) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("DROP DATABASE IF EXISTS `" + database + "`");
            }
        }
    }

    static boolean isStaleTemporaryDatabase(String database, long now) {
        if (database == null) {
            return false;
        }
        Matcher matcher = TEMPORARY_DATABASE_PATTERN.matcher(database);
        if (!matcher.matches()) {
            return false;
        }
        try {
            long createdAt = Long.parseLong(matcher.group(1));
            return createdAt >= 0 && now - createdAt > STALE_DATABASE_AGE_MILLIS;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private Reader backupReader(File file, String backupPassword) throws Exception {
        if (!BackupJob.isSqlEncryptedFile(file)) {
            return Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8);
        }
        if (backupPassword == null || backupPassword.trim().isEmpty()) {
            throw new IllegalStateException("Backup password is required to verify the encrypted backup");
        }
        byte[] encrypted;
        try (FileInputStream inputStream = new FileInputStream(file)) {
            encrypted = IOUtil.getByteByInputStream(inputStream);
        }
        byte[] decrypted = new AESCrypto(backupPassword).decrypt(encrypted);
        return new InputStreamReader(new ByteArrayInputStream(decrypted), StandardCharsets.UTF_8);
    }

    private void executeDump(Connection connection, List<String> statements) throws Exception {
        int executed = 0;
        try (Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                String normalized = sql.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
                if (normalized.startsWith("CREATE DATABASE ")
                        || normalized.startsWith("DROP DATABASE ")
                        || normalized.startsWith("USE ")) {
                    continue;
                }
                if (!allowedStatement(normalized)) {
                    throw new IllegalStateException("Unsupported statement in restore drill: "
                            + normalized.substring(0, Math.min(normalized.length(), 80)));
                }
                statement.execute(sql);
                executed++;
            }
        }
        if (executed == 0) {
            throw new IllegalStateException("Backup contains no restorable SQL statements");
        }
    }

    private boolean allowedStatement(String sql) {
        return sql.startsWith("SET ")
                || sql.startsWith("DROP TABLE ")
                || sql.startsWith("CREATE TABLE ")
                || sql.startsWith("LOCK TABLES ")
                || sql.startsWith("UNLOCK TABLES")
                || sql.startsWith("INSERT INTO ")
                || sql.startsWith("ALTER TABLE ");
    }

    private int tableCount(Connection connection, String database) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=?")) {
            statement.setString(1, database);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                int count = resultSet.getInt(1);
                if (count == 0) {
                    throw new IllegalStateException("Restore drill produced no tables");
                }
                return count;
            }
        }
    }

    private long verifyCoreTables(Connection connection, String database) throws Exception {
        long rows = 0;
        for (String table : CORE_TABLES) {
            try (PreparedStatement tableStatement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=? AND table_name=?")) {
                tableStatement.setString(1, database);
                tableStatement.setString(2, table);
                try (ResultSet resultSet = tableStatement.executeQuery()) {
                    resultSet.next();
                    if (resultSet.getInt(1) != 1) {
                        throw new IllegalStateException("Restored backup is missing core table: " + table);
                    }
                }
            }
            try (Statement countStatement = connection.createStatement();
                 ResultSet countResult = countStatement.executeQuery("SELECT COUNT(*) FROM `" + table + "`")) {
                countResult.next();
                rows += countResult.getLong(1);
            }
        }
        return rows;
    }

    static String serverJdbcUrl(String jdbcUrl) {
        String prefix = "jdbc:mysql://";
        if (!jdbcUrl.startsWith(prefix)) {
            throw new IllegalArgumentException("Only MySQL JDBC URLs are supported by restore drill");
        }
        int pathStart = jdbcUrl.indexOf('/', prefix.length());
        if (pathStart < 0) {
            return jdbcUrl + "/";
        }
        int queryStart = jdbcUrl.indexOf('?', pathStart);
        String authority = jdbcUrl.substring(0, pathStart + 1);
        return queryStart < 0 ? authority : authority + jdbcUrl.substring(queryStart);
    }

    public static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream inputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) >= 0) {
                digest.update(buffer, 0, length);
            }
        }
        StringBuilder result = new StringBuilder();
        for (byte value : digest.digest()) {
            result.append(String.format("%02x", value & 0xff));
        }
        return result.toString();
    }

    interface VerificationRecorder {
        void record(IOSession session, RestoreDrillResult result);
    }
}
