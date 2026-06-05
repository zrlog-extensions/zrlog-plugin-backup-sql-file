package com.zrlog.plugin.backup.model;

import java.util.ArrayList;
import java.util.List;

public class SiteExportPreviewResponse {

    private int schemaVersion = 1;
    private String exportId;
    private long generatedAt;
    private String packageName = "zrlog-export.zip";
    private SiteExportOptions options = new SiteExportOptions();
    private SiteExportCounts counts = new SiteExportCounts();
    private List<String> packagePaths = new ArrayList<>();
    private List<RedactionEntry> redactions = new ArrayList<>();
    private List<String> notes = new ArrayList<>();

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getExportId() {
        return exportId;
    }

    public void setExportId(String exportId) {
        this.exportId = exportId;
    }

    public long getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(long generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public SiteExportOptions getOptions() {
        return options;
    }

    public void setOptions(SiteExportOptions options) {
        this.options = options;
    }

    public SiteExportCounts getCounts() {
        return counts;
    }

    public void setCounts(SiteExportCounts counts) {
        this.counts = counts;
    }

    public List<String> getPackagePaths() {
        return packagePaths;
    }

    public void setPackagePaths(List<String> packagePaths) {
        this.packagePaths = packagePaths;
    }

    public List<RedactionEntry> getRedactions() {
        return redactions;
    }

    public void setRedactions(List<RedactionEntry> redactions) {
        this.redactions = redactions;
    }

    public List<String> getNotes() {
        return notes;
    }

    public void setNotes(List<String> notes) {
        this.notes = notes;
    }

    public static class SiteExportOptions {

        private boolean includeDrafts = true;
        private boolean includePrivateArticles = true;
        private boolean includeComments = true;
        private boolean includeMediaFiles = true;
        private boolean includeThemeFiles = true;
        private boolean includePluginConfigs = true;
        private boolean includePluginRuntimeState = false;
        private boolean includeAiMessages = false;

        public boolean isIncludeDrafts() {
            return includeDrafts;
        }

        public void setIncludeDrafts(boolean includeDrafts) {
            this.includeDrafts = includeDrafts;
        }

        public boolean isIncludePrivateArticles() {
            return includePrivateArticles;
        }

        public void setIncludePrivateArticles(boolean includePrivateArticles) {
            this.includePrivateArticles = includePrivateArticles;
        }

        public boolean isIncludeComments() {
            return includeComments;
        }

        public void setIncludeComments(boolean includeComments) {
            this.includeComments = includeComments;
        }

        public boolean isIncludeMediaFiles() {
            return includeMediaFiles;
        }

        public void setIncludeMediaFiles(boolean includeMediaFiles) {
            this.includeMediaFiles = includeMediaFiles;
        }

        public boolean isIncludeThemeFiles() {
            return includeThemeFiles;
        }

        public void setIncludeThemeFiles(boolean includeThemeFiles) {
            this.includeThemeFiles = includeThemeFiles;
        }

        public boolean isIncludePluginConfigs() {
            return includePluginConfigs;
        }

        public void setIncludePluginConfigs(boolean includePluginConfigs) {
            this.includePluginConfigs = includePluginConfigs;
        }

        public boolean isIncludePluginRuntimeState() {
            return includePluginRuntimeState;
        }

        public void setIncludePluginRuntimeState(boolean includePluginRuntimeState) {
            this.includePluginRuntimeState = includePluginRuntimeState;
        }

        public boolean isIncludeAiMessages() {
            return includeAiMessages;
        }

        public void setIncludeAiMessages(boolean includeAiMessages) {
            this.includeAiMessages = includeAiMessages;
        }
    }

    public static class SiteExportCounts {

        private long articles;
        private long publishedArticles;
        private long draftArticles;
        private long privateArticles;
        private long articleVersions;
        private long comments;
        private long types;
        private long tags;
        private long navs;
        private long links;
        private long websiteKeys;
        private long mediaFiles;
        private long mediaBytes;
        private long aiMessageArticles;
        private long aiMessageBytes;

        public long getArticles() {
            return articles;
        }

        public void setArticles(long articles) {
            this.articles = articles;
        }

        public long getPublishedArticles() {
            return publishedArticles;
        }

        public void setPublishedArticles(long publishedArticles) {
            this.publishedArticles = publishedArticles;
        }

        public long getDraftArticles() {
            return draftArticles;
        }

        public void setDraftArticles(long draftArticles) {
            this.draftArticles = draftArticles;
        }

        public long getPrivateArticles() {
            return privateArticles;
        }

        public void setPrivateArticles(long privateArticles) {
            this.privateArticles = privateArticles;
        }

        public long getArticleVersions() {
            return articleVersions;
        }

        public void setArticleVersions(long articleVersions) {
            this.articleVersions = articleVersions;
        }

        public long getComments() {
            return comments;
        }

        public void setComments(long comments) {
            this.comments = comments;
        }

        public long getTypes() {
            return types;
        }

        public void setTypes(long types) {
            this.types = types;
        }

        public long getTags() {
            return tags;
        }

        public void setTags(long tags) {
            this.tags = tags;
        }

        public long getNavs() {
            return navs;
        }

        public void setNavs(long navs) {
            this.navs = navs;
        }

        public long getLinks() {
            return links;
        }

        public void setLinks(long links) {
            this.links = links;
        }

        public long getWebsiteKeys() {
            return websiteKeys;
        }

        public void setWebsiteKeys(long websiteKeys) {
            this.websiteKeys = websiteKeys;
        }

        public long getMediaFiles() {
            return mediaFiles;
        }

        public void setMediaFiles(long mediaFiles) {
            this.mediaFiles = mediaFiles;
        }

        public long getMediaBytes() {
            return mediaBytes;
        }

        public void setMediaBytes(long mediaBytes) {
            this.mediaBytes = mediaBytes;
        }

        public long getAiMessageArticles() {
            return aiMessageArticles;
        }

        public void setAiMessageArticles(long aiMessageArticles) {
            this.aiMessageArticles = aiMessageArticles;
        }

        public long getAiMessageBytes() {
            return aiMessageBytes;
        }

        public void setAiMessageBytes(long aiMessageBytes) {
            this.aiMessageBytes = aiMessageBytes;
        }
    }

    public static class RedactionEntry {

        private String scope;
        private String key;
        private String reason;

        public RedactionEntry() {
        }

        public RedactionEntry(String scope, String key, String reason) {
            this.scope = scope;
            this.key = key;
            this.reason = reason;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
