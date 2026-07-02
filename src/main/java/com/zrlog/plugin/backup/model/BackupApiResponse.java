package com.zrlog.plugin.backup.model;

public class BackupApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    public BackupApiResponse() {
    }

    private BackupApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> BackupApiResponse<T> success(T data) {
        return new BackupApiResponse<T>(true, null, data);
    }

    public static BackupApiResponse<Void> error(String message) {
        return new BackupApiResponse<Void>(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
