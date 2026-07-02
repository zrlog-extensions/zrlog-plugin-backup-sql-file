package com.zrlog.plugin.backup.model;

import com.zrlog.plugin.message.Plugin;

import java.util.List;

public class BackupPageData {

    private boolean dark;
    private String colorPrimary;
    private Plugin plugin;
    private BackupConfig config;
    private List<BackupFileRecord> files;
    private Object history;
    private int maxKeepSize;
    private String schedulerTimezone;
    private Object schedule;
    private BackupNotificationChannels notificationChannels;
    private SiteExportPreviewResponse siteExport;
    private String siteExportError;

    public boolean isDark() {
        return dark;
    }

    public void setDark(boolean dark) {
        this.dark = dark;
    }

    public String getColorPrimary() {
        return colorPrimary;
    }

    public void setColorPrimary(String colorPrimary) {
        this.colorPrimary = colorPrimary;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    public BackupConfig getConfig() {
        return config;
    }

    public void setConfig(BackupConfig config) {
        this.config = config;
    }

    public List<BackupFileRecord> getFiles() {
        return files;
    }

    public void setFiles(List<BackupFileRecord> files) {
        this.files = files;
    }

    public Object getHistory() {
        return history;
    }

    public void setHistory(Object history) {
        this.history = history;
    }

    public int getMaxKeepSize() {
        return maxKeepSize;
    }

    public void setMaxKeepSize(int maxKeepSize) {
        this.maxKeepSize = maxKeepSize;
    }

    public String getSchedulerTimezone() {
        return schedulerTimezone;
    }

    public void setSchedulerTimezone(String schedulerTimezone) {
        this.schedulerTimezone = schedulerTimezone;
    }

    public Object getSchedule() {
        return schedule;
    }

    public void setSchedule(Object schedule) {
        this.schedule = schedule;
    }

    public BackupNotificationChannels getNotificationChannels() {
        return notificationChannels;
    }

    public void setNotificationChannels(BackupNotificationChannels notificationChannels) {
        this.notificationChannels = notificationChannels;
    }

    public SiteExportPreviewResponse getSiteExport() {
        return siteExport;
    }

    public void setSiteExport(SiteExportPreviewResponse siteExport) {
        this.siteExport = siteExport;
    }

    public String getSiteExportError() {
        return siteExportError;
    }

    public void setSiteExportError(String siteExportError) {
        this.siteExportError = siteExportError;
    }
}
