package com.zrlog.plugin.backup.scheduler;

import java.io.File;

public class BackupFileInfo {

    private File resultFile;
    private String sourceFileMd5;

    public BackupFileInfo() {
    }

    public BackupFileInfo(File resultFile, String sourceFileMd5) {
        this.resultFile = resultFile;
        this.sourceFileMd5 = sourceFileMd5;
    }

    public File getResultFile() {
        return resultFile;
    }

    public void setResultFile(File resultFile) {
        this.resultFile = resultFile;
    }

    public String getSourceFileMd5() {
        return sourceFileMd5;
    }

    public void setSourceFileMd5(String sourceFileMd5) {
        this.sourceFileMd5 = sourceFileMd5;
    }
}
