package com.zrlog.plugin.backup.scheduler;

import java.io.File;

public class BackupResultVO {
    private File file;
    private boolean newFile;
    private String dbName;

    public BackupResultVO() {
    }

    public BackupResultVO(File file, boolean newFile, String dbName) {
        this.file = file;
        this.newFile = newFile;
        this.dbName = dbName;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public boolean isNewFile() {
        return newFile;
    }

    public void setNewFile(boolean newFile) {
        this.newFile = newFile;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }
}
