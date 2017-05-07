package com.fzb.zrlog.plugin.backup.scheduler.handle;

import com.fzb.common.util.IOUtil;
import com.fzb.common.util.RunConstants;
import com.fzb.zrlog.plugin.backup.Start;
import com.fzb.zrlog.plugin.type.RunType;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class BackupExecution {

    private static final Logger LOGGER = Logger.getLogger(BackupExecution.class);

    public byte[] getDumpFileBytes(String user, String host, String dbName, String password) throws Exception {
        File binFile;
        if ("/".equals(File.separator)) {
            binFile = new File(Start.filePath + "/mysqldump");
        } else {
            binFile = new File(Start.filePath + "/mysqldump.exe");
        }
        copyInternalFileTo(BackupExecution.class.getResourceAsStream("/lib/" + binFile.getName()), binFile);
        String execString = binFile.toString() + " -h" + host + "  -u" + user + " -p" + password + " --databases " + dbName;
        if (RunConstants.runType == RunType.DEV) {
            LOGGER.info(execString);
        }
        if ("/".equals(File.separator)) {
            Runtime.getRuntime().exec("chmod 777 " + binFile);
        }
        Runtime runtime = Runtime.getRuntime();

        Process process = runtime.exec(execString);
        byte[] bytes = IOUtil.getByteByInputStream(process.getInputStream());
        process.destroy();
        return bytes;
    }

    private void copyInternalFileTo(InputStream inputStream, File file) {
        byte[] tempByte = new byte[1024];
        try {
            int length;
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(file);
                while ((length = inputStream.read(tempByte)) != -1) {
                    fileOutputStream.write(tempByte, 0, length);
                }
            } catch (IOException e) {
                LOGGER.error("stream error", e);
            } finally {
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        LOGGER.error("stream error", e);
                    }
                }
            }
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                LOGGER.error("stream error", e);
            }

        }
    }
}
