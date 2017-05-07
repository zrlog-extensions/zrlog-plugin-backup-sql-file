package com.fzb.zrlog.plugin.backup.scheduler.handle;

import com.fzb.common.util.IOUtil;
import com.fzb.common.util.RunConstants;
import com.fzb.zrlog.plugin.backup.Start;
import com.fzb.zrlog.plugin.type.RunType;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.InputStream;

public class WindowsBackupExecution implements BackupExecution {

    private static final Logger LOGGER = Logger.getLogger(UnixBackupExecution.class);

    @Override
    public byte[] getDumpFileBytes(String user, String host, String dbName, String password) throws Exception {
        File binFile = new File(Start.filePath + "/mysqldump.exe");
        copyInternalFileTo(WindowsBackupExecution.class.getResourceAsStream("/lib/mysqldump.exe"), binFile);
        String execString = binFile.toString() + " -h" + host + "  -u" + user + " -p" + password + " --databases " + dbName;
        if (RunConstants.runType == RunType.DEV) {
            LOGGER.info(execString);
        }
        Runtime runtime = Runtime.getRuntime();

        Process process = runtime.exec(execString);
        byte[] bytes = IOUtil.getByteByInputStream(process.getInputStream());
        process.destroy();
        return bytes;
    }

    private void copyInternalFileTo(InputStream inputStream, File file) {
        byte[] bytes = IOUtil.getByteByInputStream(inputStream);
        IOUtil.writeBytesToFile(bytes, file);
    }
}
