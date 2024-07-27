package com.zrlog.plugin.backup.scheduler.handle;

import com.zrlog.plugin.RunConstants;
import com.zrlog.plugin.backup.scheduler.BackupFileInfo;
import com.zrlog.plugin.backup.util.AESCrypto;
import com.zrlog.plugin.common.IOUtil;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.common.PathKit;
import com.zrlog.plugin.common.SecurityUtils;
import com.zrlog.plugin.type.RunType;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BackupExecution {

    private static final Logger LOGGER = LoggerUtil.getLogger(BackupExecution.class);

    public static void main(String[] args) throws IOException {
        System.out.println(getBinFile());
    }

    private static File getBinFile() throws IOException {
        File binFile;
        if (testMysqlDumpInstalled()) {
            binFile = new File("mysqldump");
        } else {
            String path = System.getProperties().getProperty("os.arch").replace("amd64", "x86_64") + "/" + System.getProperties().getProperty(
                    "os.name").toLowerCase().replace(" ", "") + "/mysqldump";
            binFile = new File(PathKit.getTmpPath() + "/" + path);
            if (RunConstants.runType == RunType.DEV) {
                LOGGER.info("Temp file " + binFile + ", path " + path);
            }
            copyInternalFileTo(BackupExecution.class.getResourceAsStream("/lib/" + path), binFile);
            //unix 设置执行权限
            if (path.contains("windows")) {
                return binFile;
            }
            Process process = Runtime.getRuntime().exec("chmod 777 " + binFile);
            try {
                process.waitFor();
                process.destroy();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
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
                LOGGER.log(Level.SEVERE, "UnSupport mysqldump", e);
            }
            return false;
        }
    }

    private static void copyInternalFileTo(InputStream inputStream, File file) {
        if (inputStream == null) {
            return;
        }
        byte[] tempByte = new byte[1024];
        try {
            int length;
            file.getParentFile().mkdirs();
            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                while ((length = inputStream.read(tempByte)) != -1) {
                    fileOutputStream.write(tempByte, 0, length);
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "stream error", e);
            }
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "stream error", e);
            }
        }
    }

    public BackupFileInfo dumpToFile(String user, int port, String host, String dbName, String password, String backupPassword) throws Exception {
        new File(PathKit.getTmpPath()).mkdirs();
        File file = File.createTempFile("temp", ".sql", new File(PathKit.getTmpPath()));
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            if (RunConstants.runType == RunType.DEV) {
                LOGGER.info("DumpFile start");
            }

            String execString = getBinFile() + " -f -h" + host + " -P" + port + "  -u" + user + " -p" + password + " " +
                    "--databases " + dbName;
            if (RunConstants.runType == RunType.DEV) {
                LOGGER.info(execString);
            }
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(execString);
            InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream(), Charset.defaultCharset());
            try (BufferedReader b = new BufferedReader(inputStreamReader)) {
                String line;
                while ((line = b.readLine()) != null) {
                    if (line.startsWith("-- Dump completed on")) {
                        continue;
                    }
                    fileOutputStream.write(line.getBytes());
                    fileOutputStream.write("\n".getBytes());
                }
            }
            process.destroy();
        }
        String md5 = SecurityUtils.md5ByFile(file);
        if (Objects.isNull(backupPassword) || backupPassword.trim().isEmpty()) {
            return new BackupFileInfo(file, md5);
        }
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] bytes = new AESCrypto(backupPassword).encrypt(IOUtil.getByteByInputStream(fileInputStream));
            File newFile = new File(file + ".encrypted");
            IOUtil.writeBytesToFile(bytes, newFile);
            file.delete();
            return new BackupFileInfo(newFile, md5);
        }
    }
}
