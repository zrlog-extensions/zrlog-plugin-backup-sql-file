package com.zrlog.plugin.backup.model;

public class BackupConfig {

    private String backupPassword = "";
    private String backupFilePath = "";
    private String backupCron = "";
    private String cycle = "";

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

    public String getBackupCron() {
        return backupCron;
    }

    public void setBackupCron(String backupCron) {
        this.backupCron = backupCron;
    }

    public String getCycle() {
        return cycle;
    }

    public void setCycle(String cycle) {
        this.cycle = cycle;
    }
}
