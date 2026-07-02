package com.zrlog.plugin.backup.model;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class BackupRequestParams {

    private String backupPassword;
    private String backupFilePath;
    private String source;
    private String file;
    private Object successChannels;
    private Object failedChannels;
    private Boolean includeDrafts;
    private Boolean includePrivateArticles;
    private Boolean includeComments;
    private Boolean includeMediaFiles;
    private Boolean includeThemeFiles;
    private Boolean includePluginConfigs;
    private Boolean includeAiMessages;

    public static BackupRequestParams fromParams(Function<String, Object> paramValue) {
        BackupRequestParams request = new BackupRequestParams();
        request.setBackupPassword(stringValue(paramValue.apply("backupPassword")));
        request.setBackupFilePath(stringValue(paramValue.apply("backupFilePath")));
        request.setSource(stringValue(paramValue.apply("source")));
        request.setFile(stringValue(paramValue.apply("file")));
        request.setSuccessChannels(paramValue.apply("successChannels"));
        request.setFailedChannels(paramValue.apply("failedChannels"));
        request.setIncludeDrafts(booleanValue(paramValue.apply("includeDrafts")));
        request.setIncludePrivateArticles(booleanValue(paramValue.apply("includePrivateArticles")));
        request.setIncludeComments(booleanValue(paramValue.apply("includeComments")));
        request.setIncludeMediaFiles(booleanValue(paramValue.apply("includeMediaFiles")));
        request.setIncludeThemeFiles(booleanValue(paramValue.apply("includeThemeFiles")));
        request.setIncludePluginConfigs(booleanValue(paramValue.apply("includePluginConfigs")));
        request.setIncludeAiMessages(booleanValue(paramValue.apply("includeAiMessages")));
        return request;
    }

    public BackupConfig toConfig() {
        BackupConfig config = new BackupConfig();
        config.setBackupPassword(backupPassword == null ? "" : backupPassword);
        config.setBackupFilePath(backupFilePath == null ? "" : backupFilePath);
        return config;
    }

    public SiteExportPreviewResponse.SiteExportOptions toSiteExportOptions() {
        SiteExportPreviewResponse.SiteExportOptions options = new SiteExportPreviewResponse.SiteExportOptions();
        apply(includeDrafts, value -> options.setIncludeDrafts(value));
        apply(includePrivateArticles, value -> options.setIncludePrivateArticles(value));
        apply(includeComments, value -> options.setIncludeComments(value));
        apply(includeMediaFiles, value -> options.setIncludeMediaFiles(value));
        apply(includeThemeFiles, value -> options.setIncludeThemeFiles(value));
        apply(includePluginConfigs, value -> options.setIncludePluginConfigs(value));
        options.setIncludePluginRuntimeState(false);
        apply(includeAiMessages, value -> options.setIncludeAiMessages(value));
        return options;
    }

    private interface BooleanConsumer {
        void accept(boolean value);
    }

    private void apply(Boolean value, BooleanConsumer consumer) {
        if (value != null) {
            consumer.accept(value);
        }
    }

    private static Boolean booleanValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String text = stringValue(value);
        if (text.isEmpty()) {
            return null;
        }
        return Boolean.parseBoolean(text);
    }

    private static String stringValue(Object value) {
        if (value instanceof String[]) {
            String[] values = (String[]) value;
            return values.length == 0 ? "" : values[0];
        }
        if (value instanceof List && !((List) value).isEmpty()) {
            return String.valueOf(((List) value).get(0));
        }
        return value == null ? "" : String.valueOf(value).trim();
    }

    public static List<String> channelList(Object value) {
        if (value instanceof String[]) {
            return Arrays.asList((String[]) value);
        }
        if (value instanceof List) {
            List list = (List) value;
            String[] values = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                values[i] = stringValue(list.get(i));
            }
            return Arrays.asList(values);
        }
        return Arrays.asList(stringValue(value).split(","));
    }

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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public Object getSuccessChannels() {
        return successChannels;
    }

    public void setSuccessChannels(Object successChannels) {
        this.successChannels = successChannels;
    }

    public Object getFailedChannels() {
        return failedChannels;
    }

    public void setFailedChannels(Object failedChannels) {
        this.failedChannels = failedChannels;
    }

    public Boolean getIncludeDrafts() {
        return includeDrafts;
    }

    public void setIncludeDrafts(Boolean includeDrafts) {
        this.includeDrafts = includeDrafts;
    }

    public Boolean getIncludePrivateArticles() {
        return includePrivateArticles;
    }

    public void setIncludePrivateArticles(Boolean includePrivateArticles) {
        this.includePrivateArticles = includePrivateArticles;
    }

    public Boolean getIncludeComments() {
        return includeComments;
    }

    public void setIncludeComments(Boolean includeComments) {
        this.includeComments = includeComments;
    }

    public Boolean getIncludeMediaFiles() {
        return includeMediaFiles;
    }

    public void setIncludeMediaFiles(Boolean includeMediaFiles) {
        this.includeMediaFiles = includeMediaFiles;
    }

    public Boolean getIncludeThemeFiles() {
        return includeThemeFiles;
    }

    public void setIncludeThemeFiles(Boolean includeThemeFiles) {
        this.includeThemeFiles = includeThemeFiles;
    }

    public Boolean getIncludePluginConfigs() {
        return includePluginConfigs;
    }

    public void setIncludePluginConfigs(Boolean includePluginConfigs) {
        this.includePluginConfigs = includePluginConfigs;
    }

    public Boolean getIncludeAiMessages() {
        return includeAiMessages;
    }

    public void setIncludeAiMessages(Boolean includeAiMessages) {
        this.includeAiMessages = includeAiMessages;
    }
}
