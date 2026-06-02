package com.zrlog.plugin.backup.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BackupNotificationChannels {

    public static final String STORE_KEY = "plugin.backupSqlFile.notification.channels";
    public static final String SCHEMA = STORE_KEY;
    private static final List<String> FALLBACK_CHANNELS = Arrays.asList("email");

    private String schema = SCHEMA;
    private int version = 1;
    private BackupNotificationChannelData data = new BackupNotificationChannelData();

    public static BackupNotificationChannels defaults() {
        return normalize(new BackupNotificationChannels());
    }

    public static BackupNotificationChannels normalize(BackupNotificationChannels channels) {
        BackupNotificationChannels normalized = channels == null ? new BackupNotificationChannels() : channels;
        normalized.setSchema(SCHEMA);
        if (normalized.getVersion() <= 0) {
            normalized.setVersion(1);
        }
        BackupNotificationChannelData data = normalized.getData();
        if (data == null) {
            data = new BackupNotificationChannelData();
            normalized.setData(data);
        }
        data.setSuccessChannels(normalizeChannels(data.getSuccessChannels(), FALLBACK_CHANNELS));
        data.setFailedChannels(normalizeChannels(data.getFailedChannels(), data.getSuccessChannels()));
        return normalized;
    }

    public List<String> successChannels() {
        return copy(normalize(this).getData().getSuccessChannels());
    }

    public List<String> failedChannels() {
        return copy(normalize(this).getData().getFailedChannels());
    }

    private static List<String> normalizeChannels(List<String> channels, List<String> fallback) {
        List<String> values = new ArrayList<String>();
        if (channels != null) {
            for (String channel : channels) {
                if (channel == null) {
                    continue;
                }
                String text = channel.trim();
                if (!text.isEmpty() && !values.contains(text)) {
                    values.add(text);
                }
            }
        }
        if (values.isEmpty()) {
            values.addAll(fallback == null || fallback.isEmpty() ? FALLBACK_CHANNELS : fallback);
        }
        return values;
    }

    private static List<String> copy(List<String> values) {
        return new ArrayList<String>(values == null || values.isEmpty() ? FALLBACK_CHANNELS : values);
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public BackupNotificationChannelData getData() {
        return data;
    }

    public void setData(BackupNotificationChannelData data) {
        this.data = data;
    }

    public static class BackupNotificationChannelData {
        private List<String> successChannels = new ArrayList<String>(FALLBACK_CHANNELS);
        private List<String> failedChannels = new ArrayList<String>(FALLBACK_CHANNELS);

        public List<String> getSuccessChannels() {
            return successChannels;
        }

        public void setSuccessChannels(List<String> successChannels) {
            this.successChannels = successChannels;
        }

        public List<String> getFailedChannels() {
            return failedChannels;
        }

        public void setFailedChannels(List<String> failedChannels) {
            this.failedChannels = failedChannels;
        }
    }
}
