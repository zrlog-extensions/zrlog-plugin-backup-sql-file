package com.zrlog.plugin.backup.scheduler.handle;

import com.hibegin.common.util.IOUtil;
import com.zrlog.plugin.RunConstants;
import com.zrlog.plugin.common.PathKit;
import com.zrlog.plugin.type.RunType;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class BackupExecution {

    private static final Logger LOGGER = Logger.getLogger(BackupExecution.class);

    public static void main(String[] args) throws IOException {
        System.out.println(getBinFile());
    }

    private static File getBinFile() throws IOException {
        File binFile;
        if (testMysqlDumpInstalled()) {
            binFile = new File("mysqldump");
        } else {
            String path = System.getProperties().getProperty("os.arch") + "/" + System.getProperties().getProperty(
                    "os.name").toLowerCase() + "/mysqldump";
            binFile = new File(PathKit.getTmpPath() + "/" + path);
            LOGGER.info("Temp file " + path);
            copyInternalFileTo(BackupExecution.class.getResourceAsStream("/lib/" + path), binFile);
            //unix 设置执行权限
            if ("/".equals(File.separator)) {
                Runtime.getRuntime().exec("chmod 777 " + binFile);
            }
        }
        return binFile;
    }

    /**
     * 系统内是否安装了 mysqlDump
     */
    private static boolean testMysqlDumpInstalled() {
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("mysqldump");
            process.destroy();
            return true;
        } catch (IOException e) {
            if (RunConstants.runType == RunType.DEV) {
                LOGGER.error("unSupport mysqldump", e);
            }
            return false;
        }
    }

    private static void copyInternalFileTo(InputStream inputStream, File file) {
        byte[] tempByte = new byte[1024];
        try {
            int length;
            FileOutputStream fileOutputStream = null;
            try {
                file.getParentFile().mkdirs();
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

    public byte[] getDumpFileBytes(String user, int port, String host, String dbName, String password) throws Exception {
        if (RunConstants.runType == RunType.DEV) {
            LOGGER.info("dumpFile start");
        }

        String execString =
                getBinFile().toString() + " -h" + host + " -P" + port + "  -u" + user + " -p" + password + " " +
                        "--databases " + dbName;
        if (RunConstants.runType == RunType.DEV) {
            LOGGER.info(execString);
        }
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(execString);
        byte[] bytes = IOUtil.getByteByInputStream(process.getInputStream());
        if (bytes.length == 0) {
            bytes = IOUtil.getByteByInputStream(process.getErrorStream());
            LOGGER.error("the system not support mysqldump cmd \n" + new String(bytes));
        }
        process.destroy();
        return bytes;
    }
}
