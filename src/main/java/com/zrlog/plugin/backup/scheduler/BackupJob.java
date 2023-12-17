package com.zrlog.plugin.backup.scheduler;

import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.backup.Application;
import com.zrlog.plugin.backup.scheduler.handle.BackupExecution;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.common.SecurityUtils;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.type.ActionType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BackupJob implements Runnable {

    private static final Logger LOGGER = LoggerUtil.getLogger(BackupJob.class);

    private final IOSession ioSession;
    private final String propFile;
    private String backupPath;

    public BackupJob(IOSession ioSession, String propFile) {
        this.ioSession = ioSession;
        this.propFile = propFile;
    }

    public BackupResultVO backup(String backupFilePath, String backupPassword) throws Exception {
        try (FileInputStream fileInputStream = new FileInputStream(propFile)) {
            Properties properties = new Properties();
            properties.load(fileInputStream);
            URI uri = new URI(properties.getProperty("jdbcUrl").replace("jdbc:", ""));
            String dbName = uri.getPath().replace("/", "");
            StringJoiner sj = new StringJoiner("_");
            sj.add(dbName);
            sj.add(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
            sj.add(UUID.randomUUID().toString().replace("-", ""));
            File dbFile = new File(backupFilePath + "/" + sj + ".sql" + (Objects.nonNull(backupPassword) && !backupPassword.trim().isEmpty() ? ".encrypted" : ""));
            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }
            BackupExecution backupExecution = new BackupExecution();
            File dumpFile = backupExecution.dumpToFile(properties.getProperty("user"), uri.getPort(), uri.getHost(), dbName, properties.getProperty("password"), backupPassword);
            String newFileMd5 = SecurityUtils.md5ByFile(dumpFile);
            for (File file : dbFile.getParentFile().listFiles()) {
                if (Objects.equals(newFileMd5, SecurityUtils.md5ByFile(file))) {
                    dumpFile.delete();
                    return new BackupResultVO(file, false);
                }
            }
            dumpFile.renameTo(dbFile);
            return new BackupResultVO(dbFile, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            //do clear files
            clearFile();
        }
    }

    private void backupThenStoreToPrivateStore(String backupFilePath, String backupPassword) throws Exception {
        BackupResultVO resultVO = backup(backupFilePath, backupPassword);
        if (!resultVO.newFile()) {
            return;
        }
        try (FileInputStream fileInputStream = new FileInputStream(propFile)) {
            Properties properties = new Properties();
            properties.load(fileInputStream);
            URI uri = new URI(properties.getProperty("jdbcUrl").replace("jdbc:", ""));
            String dbName = uri.getPath().replace("/", "");
            Map<String, String[]> map = new HashMap<>();
            map.put("fileInfo", new String[]{resultVO.file() + "," + dbName + "/" + resultVO.file().getName()});
            ioSession.requestService("uploadToPrivateService", map);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "UploadToPrivate error", e);
        }
    }

    public static boolean isSqlFile(File file) {
        return file.getName().endsWith(".sql") || file.getName().endsWith(".encrypted");
    }

    private void clearFile() {
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
                this.backupPath = Application.sqlPath;
            } else {
                this.backupPath = backupFilePath;
            }
            backupThenStoreToPrivateStore(backupPath, responseMap.get("backupPassword"));
        } catch (URISyntaxException e) {
            LOGGER.log(Level.SEVERE, "jdbcUrl error", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "", e);
        }
    }
}
