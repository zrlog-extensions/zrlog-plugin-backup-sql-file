package com.fzb.zrlog.plugin.backup.scheduler;

import com.fzb.common.util.IOUtil;
import com.fzb.common.util.RunConstants;
import com.fzb.zrlog.plugin.backup.Start;
import com.fzb.zrlog.plugin.type.RunType;
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

public class BackUpJob implements Job {

    private static Logger LOGGER = Logger.getLogger(BackUpJob.class);

    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        LOGGER.info("Job is run");
        String execString;
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
            execString = "mysqldump -h" + uri.getHost() + "  -u" + prop.getProperty("user") + " -p" + prop.getProperty("password") + " --databases " + dbName;
            if (RunConstants.runType == RunType.DEV) {
                LOGGER.info(execString);
            }
            Runtime runtime = Runtime.getRuntime();

            Process process = runtime.exec(execString);
            byte[] bytes = IOUtil.getByteByInputStream(process.getInputStream());
            LOGGER.info("file size " + bytes.length);
            IOUtil.writeBytesToFile(bytes, dbFile);
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
