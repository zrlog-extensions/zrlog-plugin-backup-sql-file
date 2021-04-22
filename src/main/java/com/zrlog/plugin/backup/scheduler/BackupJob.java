package com.zrlog.plugin.backup.scheduler;

import com.hibegin.common.util.IOUtil;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.backup.Start;
import com.zrlog.plugin.backup.scheduler.handle.BackupExecution;
import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.*;

public class BackupJob implements Job {

    private static final Logger LOGGER = Logger.getLogger(BackupJob.class);

    public static File backupThenStoreToPrivateStore(IOSession ioSession, Properties properties) throws Exception {
        URI uri = new URI(properties.getProperty("jdbcUrl").replace("jdbc:", ""));
        String dbName = uri.getPath().replace("/", "");
        File dbFile =
                new File(Start.sqlPath + dbName + "_" + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) +
                        ".sql");
        if (!dbFile.getParentFile().exists()) {
            dbFile.getParentFile().mkdirs();
        }
        BackupExecution backupExecution = new BackupExecution();
        byte[] dumpFileBytes = backupExecution.getDumpFileBytes(properties.getProperty("user"), uri.getPort(),
                uri.getHost(), dbName, properties.getProperty("password"));
        IOUtil.writeBytesToFile(dumpFileBytes, dbFile);
        try {
            Map<String, String[]> map = new HashMap<>();
            map.put("fileInfo", new String[]{dbFile + "," + dbName + "/" + dbFile.getName()});
            ioSession.requestService("uploadToPrivateService", map);
        } catch (Exception e) {
            LOGGER.info("uploadToPrivate error", e);
        }
        return dbFile;
    }

    public static void clearFile() {
        File dbPath = new File(Start.sqlPath);
        if (dbPath.exists()) {
            File[] files = dbPath.listFiles();
            if (files != null) {
                List<File> fileList = new ArrayList<>();
                for (File file : files) {
                    if (file.getName().endsWith(".sql")) {
                        fileList.add(file);
                    }
                }
                if (fileList.size() > Start.maxBackupSqlFileCount) {
                    fileList.sort(Comparator.comparingLong(File::lastModified));
                    List<File> needRemoveFileList = fileList.subList(0, fileList.size() - Start.maxBackupSqlFileCount);
                    for (File file : needRemoveFileList) {
                        file.delete();
                    }
                }
            }
        }
    }

    @Override
    public void execute(JobExecutionContext context) {
        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream(context.getJobDetail().getJobDataMap().get("dbProperties").toString()));
            IOSession ioSession = (IOSession) context.getJobDetail().getJobDataMap().get("ioSession");
            backupThenStoreToPrivateStore(ioSession, prop);
        } catch (URISyntaxException e) {
            LOGGER.error("jdbcUrl error", e);
        } catch (Exception e) {
            LOGGER.error("", e);
        } finally {
            clearFile();
        }
    }
}
