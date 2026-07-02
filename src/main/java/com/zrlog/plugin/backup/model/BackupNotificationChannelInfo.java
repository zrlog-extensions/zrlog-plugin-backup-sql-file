package com.zrlog.plugin.backup.model;

import com.zrlog.plugin.message.NotificationChannelProvider;

import java.util.List;

public class BackupNotificationChannelInfo {

    private BackupNotificationChannels settings;
    private List<NotificationChannelProvider> providers;

    public BackupNotificationChannels getSettings() {
        return settings;
    }

    public void setSettings(BackupNotificationChannels settings) {
        this.settings = settings;
    }

    public List<NotificationChannelProvider> getProviders() {
        return providers;
    }

    public void setProviders(List<NotificationChannelProvider> providers) {
        this.providers = providers;
    }
}
