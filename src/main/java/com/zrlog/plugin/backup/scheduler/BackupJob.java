package com.zrlog.plugin.backup.scheduler;

import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.backup.Application;
import com.zrlog.plugin.backup.scheduler.handle.BackupExecution;
import com.zrlog.plugin.backup.util.AESCrypto;
import com.zrlog.plugin.common.IOUtil;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.common.SecurityUtils;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.type.ActionType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BackupJob implements Runnable {

    private static final Logger LOGGER = LoggerUtil.getLogger(BackupJob.class);

    private final IOSession ioSession;

    public BackupJob(IOSession ioSession) {
        this.ioSession = ioSession;
    }

    public BackupResultVO backup(String backupFilePath, String backupPassword) throws Exception {
        Map responseSync = ioSession.getResponseSync(ContentType.JSON, new HashMap<>(), ActionType.GET_DB_PROPERTIES, Map.class);
        try (FileInputStream fileInputStream = new FileInputStream((String) responseSync.get("dbProperties"))) {
            Properties properties = new Properties();
            properties.load(fileInputStream);
            URI uri = new URI(properties.getProperty("jdbcUrl").replace("jdbc:", ""));
            String dbName = uri.getPath().replace("/", "");


            BackupExecution backupExecution = new BackupExecution();
            BackupFileInfo backupFileInfo = backupExecution.dumpToFile(properties.getProperty("user"), uri.getPort(), uri.getHost(), dbName, properties.getProperty("password"), backupPassword);
            String newFileMd5 = backupFileInfo.sourceFileMd5();
            File dumpFile = backupFileInfo.resultFile();
            StringJoiner sj = new StringJoiner("_");
            sj.add(dbName);
            sj.add(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
            sj.add(UUID.randomUUID().toString().replace("-", ""));
            File dbFile = new File(backupFilePath + "/" + sj + ".sql" + (Objects.nonNull(backupPassword) && !backupPassword.trim().isEmpty() ? ".encrypted" : ""));
            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }
            for (File file : dbFile.getParentFile().listFiles()) {
                if (!isSqlFile(file)) {
                    continue;
                }
                if (isSqlEncryptedFile(file)) {
                    try {
                        String resource = SecurityUtils.md5(new AESCrypto(backupPassword).decrypt(IOUtil.getByteByInputStream(new FileInputStream(file))));
                        if (Objects.equals(resource, newFileMd5)) {
                            dumpFile.delete();
                            return new BackupResultVO(file, false, dbName);
                        }
                    } catch (Exception e) {
                        //ignore
                    }
                }
                if (Objects.equals(newFileMd5, SecurityUtils.md5ByFile(file))) {
                    dumpFile.delete();
                    return new BackupResultVO(file, false, dbName);
                }
            }
            Files.move(dumpFile.toPath(), dbFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return new BackupResultVO(dbFile, true, dbName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            //do clear files
            clearFile(backupFilePath);
        }
    }

    private void backupThenStoreToPrivateStore(String backupFilePath, String backupPassword) throws Exception {
        BackupResultVO resultVO = backup(backupFilePath, backupPassword);
        if (!resultVO.newFile()) {
            return;
        }
        try {
            Map<String, String[]> map = new HashMap<>();
            map.put("fileInfo", new String[]{resultVO.file() + "," + resultVO.dbName() + "/" + resultVO.file().getName()});
            ioSession.requestService("uploadToPrivateService", map);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "UploadToPrivate error", e);
        }
    }

    public static boolean isSqlFile(File file) {
        return file.getName().endsWith(".sql") || isSqlEncryptedFile(file);
    }

    public static boolean isSqlEncryptedFile(File file) {
        return file.getName().endsWith(".sql.encrypted");
    }

    private void clearFile(String backupPath) {
        if (Objects.isNull(backupPath)) {
            return;
        }
        File dbPath = new File(backupPath);
        if (!dbPath.exists()) {
            return;
        }
        File[] files = dbPath.listFiles();
        if (Objects.isNull(files)) {
            return;
        }
        List<File> fileList = new ArrayList<>();
        for (File file : files) {
            if (isSqlFile(file)) {
                fileList.add(file);
            }
        }
        if (fileList.size() > Application.maxBackupSqlFileCount) {
            fileList.sort(Comparator.comparingLong(File::lastModified));
            List<File> needRemoveFileList = fileList.subList(0, fileList.size() - Application.maxBackupSqlFileCount);
            for (File file : needRemoveFileList) {
                file.delete();
            }
        }
    }

    @Override
    public void run() {
        try {
            Map<String, String> map = new HashMap<>();
            map.put("key", "backupPassword,backupFilePath");
            Map<String, String> responseMap = ioSession.getResponseSync(ContentType.JSON, map, ActionType.GET_WEBSITE, Map.class);
            String backupFilePath = responseMap.get("backupFilePath");
            if (Objects.isNull(backupFilePath) || backupFilePath.isEmpty()) {
                backupFilePath = Application.sqlPath;
            }
            backupThenStoreToPrivateStore(backupFilePath, responseMap.get("backupPassword"));
        } catch (URISyntaxException e) {
            LOGGER.log(Level.SEVERE, "jdbcUrl error", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "", e);
        }
    }

    public static void main(String[] args) throws IOException {
        File.createTempFile("test", "tt").renameTo(new File("/tmp/1"));
    }
}
