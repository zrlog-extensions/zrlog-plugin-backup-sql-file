package com.zrlog.plugin.backup.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BackupNotificationChannels {

    public static final String SUCCESS_CHANNELS_KEY = "notificationSuccessChannels";
    public static final String FAILED_CHANNELS_KEY = "notificationFailedChannels";
    private static final List<String> FALLBACK_CHANNELS = Arrays.asList("email");

    private List<String> successChannels = new ArrayList<String>(FALLBACK_CHANNELS);
    private List<String> failedChannels = new ArrayList<String>(FALLBACK_CHANNELS);

    public static BackupNotificationChannels defaults() {
        return normalize(new BackupNotificationChannels());
    }

    public static BackupNotificationChannels normalize(BackupNotificationChannels channels) {
        BackupNotificationChannels normalized = channels == null ? new BackupNotificationChannels() : channels;
        normalized.setSuccessChannels(normalizeChannels(normalized.getSuccessChannels(), FALLBACK_CHANNELS));
        normalized.setFailedChannels(normalizeChannels(normalized.getFailedChannels(), normalized.getSuccessChannels()));
        return normalized;
    }

    public List<String> successChannels() {
        return copy(normalize(this).getSuccessChannels());
    }

    public List<String> failedChannels() {
        return copy(normalize(this).getFailedChannels());
    }

    public static List<String> decodeChannels(String text, List<String> fallback) {
        if (text == null || text.trim().isEmpty()) {
            return normalizeChannels(null, fallback);
        }
        return normalizeChannels(Arrays.asList(text.split(",")), fallback);
    }

    public static String encodeChannels(List<String> channels) {
        return String.join(",", normalizeChannels(channels, FALLBACK_CHANNELS));
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
