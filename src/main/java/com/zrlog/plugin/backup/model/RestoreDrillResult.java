package com.zrlog.plugin.backup.model;

public class RestoreDrillResult {

    private boolean success;
    private long startedAt;
    private long completedAt;
    private String fileName;
    private String fileSha256;
    private int restoredTableCount;
    private long restoredCoreRowCount;
    private String message;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileSha256() {
        return fileSha256;
    }

    public void setFileSha256(String fileSha256) {
        this.fileSha256 = fileSha256;
    }

    public int getRestoredTableCount() {
        return restoredTableCount;
    }

    public void setRestoredTableCount(int restoredTableCount) {
        this.restoredTableCount = restoredTableCount;
    }

    public long getRestoredCoreRowCount() {
        return restoredCoreRowCount;
    }

    public void setRestoredCoreRowCount(long restoredCoreRowCount) {
        this.restoredCoreRowCount = restoredCoreRowCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
