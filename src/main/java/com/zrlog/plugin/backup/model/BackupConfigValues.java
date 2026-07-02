package com.zrlog.plugin.backup.model;

public class BackupConfigValues {

    private String backupPassword;
    private String backupFilePath;
    private String syncHistory;

    public String getBackupPassword() {
        return backupPassword;
    }

    public void setBackupPassword(String backupPassword) {
        this.backupPassword = backupPassword;
    }

    public String getBackupFilePath() {
        return backupFilePath;
    }

    public void setBackupFilePath(String backupFilePath) {
        this.backupFilePath = backupFilePath;
    }

    public String getSyncHistory() {
        return syncHistory;
    }

    public void setSyncHistory(String syncHistory) {
        this.syncHistory = syncHistory;
    }
}
