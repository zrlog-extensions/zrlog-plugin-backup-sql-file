package com.fzb.zrlog.plugin.backup.scheduler;

import com.fzb.common.util.IOUtil;
import com.fzb.zrlog.plugin.backup.Start;
import com.fzb.zrlog.plugin.common.PathKit;
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
        System.out.println("Job is run");
        String execString = "";
        File dbFile = new File(Start.filePath + new SimpleDateFormat("YYYYMMdd_HHmm").format(new Date()) + ".sql");
        try {
            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }
            dbFile.createNewFile();
            Properties prop = new Properties();
            prop.load(new FileInputStream(new File(context.getJobDetail().getJobDataMap().get("dbProperties").toString())));
            URI uri = new URI(prop.getProperty("jdbcUrl").replace("jdbc:", ""));
            execString = "mysqldump -h" + uri.getHost() + "  -u" + prop.getProperty("user") + " -p" + prop.getProperty("password") + " --databases " + uri.getPath().replace("/", "");
            Runtime runtime = Runtime.getRuntime();

            try {
                Process process = runtime.exec(execString);
                process.waitFor();
                byte[] bytes = IOUtil.getByteByInputStream(process.getInputStream());
                LOGGER.info("file size " + bytes.length);
                IOUtil.writeBytesToFile(bytes, dbFile);
            } catch (IOException | InterruptedException var6) {
                var6.printStackTrace();
            }
        } catch (IOException e) {
            LOGGER.error("unSupport mysqldump", e);
        } catch (URISyntaxException e) {
            LOGGER.error("jdbcUrl error", e);
        } finally {
            System.out.println(execString);
            /*try {
                MailUtil.sendMail(ZrlogPublicQuery.getwebSiteMap().get("backupSqlFile_mail").toString(), "定时备份SQL文件", console, f);
            } catch (Exception e) {
                LOGGER.error("sendMail error", e);
            }*/
            // send file service
        }
    }
}
