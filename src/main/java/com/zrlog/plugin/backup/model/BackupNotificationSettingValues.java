package com.zrlog.plugin.backup.model;

public class BackupNotificationSettingValues {

    private String notificationSuccessChannels;
    private String notificationFailedChannels;

    public String getNotificationSuccessChannels() {
        return notificationSuccessChannels;
    }

    public void setNotificationSuccessChannels(String notificationSuccessChannels) {
        this.notificationSuccessChannels = notificationSuccessChannels;
    }

    public String getNotificationFailedChannels() {
        return notificationFailedChannels;
    }

    public void setNotificationFailedChannels(String notificationFailedChannels) {
        this.notificationFailedChannels = notificationFailedChannels;
    }
}
