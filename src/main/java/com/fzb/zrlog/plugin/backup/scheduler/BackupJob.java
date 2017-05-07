package com.fzb.zrlog.plugin.backup.scheduler;

import com.fzb.common.util.IOUtil;
import com.fzb.zrlog.plugin.backup.Start;
import com.fzb.zrlog.plugin.backup.scheduler.handle.BackupExecution;
import com.fzb.zrlog.plugin.backup.scheduler.handle.UnixBackupExecution;
import com.fzb.zrlog.plugin.backup.scheduler.handle.WindowsBackupExecution;
import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class BackupJob implements Job {

    private static Logger LOGGER = Logger.getLogger(BackupJob.class);

    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        LOGGER.info("Job is run");
        File dbFile = null;
        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream(new File(context.getJobDetail().getJobDataMap().get("dbProperties").toString())));
            URI uri = new URI(prop.getProperty("jdbcUrl").replace("jdbc:", ""));
            String dbName = uri.getPath().replace("/", "");
            dbFile = new File(Start.filePath + dbName + "_" + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".sql");
            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }
            dbFile.createNewFile();
            BackupExecution backupExecution;
            if ("/".equals(File.separator)) {
                backupExecution = new UnixBackupExecution();
            } else {
                backupExecution = new WindowsBackupExecution();
            }
            byte[] dumpFileBytes = backupExecution.getDumpFileBytes(prop.getProperty("user"), uri.getHost(), dbName, prop.getProperty("password"));
            IOUtil.writeBytesToFile(dumpFileBytes, dbFile);
        } catch (IOException e) {
            LOGGER.error("unSupport mysqldump", e);
            if (dbFile != null) {
                dbFile.delete();
            }
        } catch (URISyntaxException e) {
            LOGGER.error("jdbcUrl error", e);
        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }
}
