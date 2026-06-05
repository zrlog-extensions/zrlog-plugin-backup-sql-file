package com.zrlog.plugin.backup.model;

import java.util.ArrayList;
import java.util.List;

public class SiteImportPrecheckResponse {

    private boolean validPackage;
    private String packageName;
    private int schemaVersion;
    private String exportId;
    private long generatedAt;
    private SiteExportPreviewResponse.SiteExportOptions options = new SiteExportPreviewResponse.SiteExportOptions();
    private SiteExportPreviewResponse.SiteExportCounts counts = new SiteExportPreviewResponse.SiteExportCounts();
    private List<String> packagePaths = new ArrayList<>();
    private long articleRows;
    private long articleAliasConflicts;
    private long aiMessageRows;
    private long aiMessageIncludedRows;
    private long aiMessageExcludedRows;
    private List<PrecheckEntry> checks = new ArrayList<>();

    public boolean isValidPackage() {
        return validPackage;
    }

    public void setValidPackage(boolean validPackage) {
        this.validPackage = validPackage;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

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

    public SiteExportPreviewResponse.SiteExportOptions getOptions() {
        return options;
    }

    public void setOptions(SiteExportPreviewResponse.SiteExportOptions options) {
        this.options = options;
    }

    public SiteExportPreviewResponse.SiteExportCounts getCounts() {
        return counts;
    }

    public void setCounts(SiteExportPreviewResponse.SiteExportCounts counts) {
        this.counts = counts;
    }

    public List<String> getPackagePaths() {
        return packagePaths;
    }

    public void setPackagePaths(List<String> packagePaths) {
        this.packagePaths = packagePaths;
    }

    public long getArticleRows() {
        return articleRows;
    }

    public void setArticleRows(long articleRows) {
        this.articleRows = articleRows;
    }

    public long getArticleAliasConflicts() {
        return articleAliasConflicts;
    }

    public void setArticleAliasConflicts(long articleAliasConflicts) {
        this.articleAliasConflicts = articleAliasConflicts;
    }

    public long getAiMessageRows() {
        return aiMessageRows;
    }

    public void setAiMessageRows(long aiMessageRows) {
        this.aiMessageRows = aiMessageRows;
    }

    public long getAiMessageIncludedRows() {
        return aiMessageIncludedRows;
    }

    public void setAiMessageIncludedRows(long aiMessageIncludedRows) {
        this.aiMessageIncludedRows = aiMessageIncludedRows;
    }

    public long getAiMessageExcludedRows() {
        return aiMessageExcludedRows;
    }

    public void setAiMessageExcludedRows(long aiMessageExcludedRows) {
        this.aiMessageExcludedRows = aiMessageExcludedRows;
    }

    public List<PrecheckEntry> getChecks() {
        return checks;
    }

    public void setChecks(List<PrecheckEntry> checks) {
        this.checks = checks;
    }

    public static class PrecheckEntry {

        private String scope;
        private String key;
        private String status;
        private String detail;

        public PrecheckEntry() {
        }

        public PrecheckEntry(String scope, String key, String status, String detail) {
            this.scope = scope;
            this.key = key;
            this.status = status;
            this.detail = detail;
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

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getDetail() {
            return detail;
        }

        public void setDetail(String detail) {
            this.detail = detail;
        }
    }
}
