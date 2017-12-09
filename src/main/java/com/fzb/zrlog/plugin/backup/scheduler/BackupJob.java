package com.fzb.zrlog.plugin.backup.scheduler;

import com.fzb.common.util.IOUtil;
import com.fzb.zrlog.plugin.backup.Start;
import com.fzb.zrlog.plugin.backup.scheduler.handle.BackupExecution;
import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.*;

public class BackupJob implements Job {

    private static Logger LOGGER = Logger.getLogger(BackupJob.class);

    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        LOGGER.info("Job is run");

        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream(new File(context.getJobDetail().getJobDataMap().get("dbProperties").toString())));
            URI uri = new URI(prop.getProperty("jdbcUrl").replace("jdbc:", ""));
            String dbName = uri.getPath().replace("/", "");
            File dbFile = new File(Start.filePath + dbName + "_" + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".sql");
            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }
            BackupExecution backupExecution = new BackupExecution();
            byte[] dumpFileBytes = backupExecution.getDumpFileBytes(prop.getProperty("user"), uri.getPort(),
                    uri.getHost(), dbName, prop.getProperty("password"));
            IOUtil.writeBytesToFile(dumpFileBytes, dbFile);
        } catch (URISyntaxException e) {
            LOGGER.error("jdbcUrl error", e);
        } catch (Exception e) {
            LOGGER.error("", e);
        } finally {
            clearFile();
        }
    }

    public void clearFile() {
        File dbPath = new File(Start.filePath);
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
                    Collections.sort(fileList, new Comparator<File>() {
                        @Override
                        public int compare(File o1, File o2) {
                            return Long.compare(o1.lastModified(), o2.lastModified());
                        }
                    });
                    List<File> needRemoveFileList = fileList.subList(0, fileList.size() - Start.maxBackupSqlFileCount);
                    for (File file : needRemoveFileList) {
                        file.delete();
                    }
                }
            }
        }
    }
}
