package com.zrlog.plugin.backup.service;

import com.zrlog.plugin.backup.model.RestoreDrillResult;
import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RestoreDrillMysqlIntegrationTest {

    @Test
    public void shouldRestoreVerifyAndRemoveTemporaryDatabase() throws Exception {
        String jdbcUrl = System.getenv("ZRLOG_RESTORE_TEST_JDBC_URL");
        Assume.assumeTrue(jdbcUrl != null && !jdbcUrl.trim().isEmpty());
        String user = System.getenv("ZRLOG_RESTORE_TEST_USER");
        String password = System.getenv("ZRLOG_RESTORE_TEST_PASSWORD");
        Class.forName("com.mysql.cj.jdbc.Driver");
        long databasesBefore = temporaryDatabaseCount(jdbcUrl, user, password);
        File dump = File.createTempFile("zrlog-restore-integration", ".sql");
        try {
            Files.write(dump.toPath(), dumpSql().getBytes(StandardCharsets.UTF_8));
            Properties properties = new Properties();
            properties.setProperty("jdbcUrl", jdbcUrl);
            properties.setProperty("user", user);
            properties.setProperty("password", password == null ? "" : password);
            RestoreDrillResult result = new RestoreDrillResult();

            new RestoreDrillService().restoreAndVerify(dump, null, properties, result);

            assertEquals(3, result.getRestoredTableCount());
            assertEquals(3, result.getRestoredCoreRowCount());
            assertEquals(databasesBefore, temporaryDatabaseCount(jdbcUrl, user, password));
        } finally {
            Files.deleteIfExists(dump.toPath());
        }
    }

    private long temporaryDatabaseCount(String jdbcUrl, String user, String password) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                RestoreDrillService.serverJdbcUrl(jdbcUrl), user, password == null ? "" : password);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT COUNT(*) FROM information_schema.schemata "
                             + "WHERE schema_name LIKE 'zrlog_restore_drill_%'")) {
            assertTrue(resultSet.next());
            return resultSet.getLong(1);
        }
    }

    private String dumpSql() {
        return "CREATE TABLE `log` (`id` int primary key, `content` text);\n"
                + "INSERT INTO `log` VALUES (1, 'hello');\n"
                + "CREATE TABLE `user` (`id` int primary key, `name` varchar(32));\n"
                + "INSERT INTO `user` VALUES (1, 'admin');\n"
                + "CREATE TABLE `website` (`id` int primary key, `name` varchar(64));\n"
                + "INSERT INTO `website` VALUES (1, 'title');\n";
    }
}
