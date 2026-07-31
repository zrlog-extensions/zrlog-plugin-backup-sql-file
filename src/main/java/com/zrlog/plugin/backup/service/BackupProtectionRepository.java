package com.zrlog.plugin.backup.service;

import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.backup.model.RestoreDrillResult;
import com.zrlog.plugin.backup.scheduler.BackupRunResult;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.type.ActionType;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BackupProtectionRepository {

    public static final String LAST_BACKUP_AT = "backupProtectionLastBackupAt";
    public static final String LAST_BACKUP_FILE = "backupProtectionLastBackupFile";
    public static final String LAST_BACKUP_SHA256 = "backupProtectionLastBackupSha256";
    public static final String LAST_VERIFIED_AT = "backupProtectionLastVerifiedAt";
    public static final String LAST_VERIFIED_FILE = "backupProtectionLastVerifiedFile";
    public static final String LAST_VERIFIED_SHA256 = "backupProtectionLastVerifiedSha256";
    public static final String LAST_VERIFICATION_SUCCESS = "backupProtectionLastVerificationSuccess";
    public static final String LAST_VERIFICATION_MESSAGE = "backupProtectionLastVerificationMessage";

    private BackupProtectionRepository() {
    }

    public static void recordBackup(IOSession session, BackupRunResult result) {
        if (result == null || !result.isSuccess()) {
            return;
        }
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(LAST_BACKUP_AT, String.valueOf(result.getCompletedAt()));
        values.put(LAST_BACKUP_FILE, result.getFileName());
        values.put(LAST_BACKUP_SHA256, result.getFileSha256());
        save(session, values);
    }

    public static void recordVerification(IOSession session, RestoreDrillResult result) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(LAST_VERIFIED_AT, String.valueOf(result.getCompletedAt()));
        values.put(LAST_VERIFIED_FILE, result.getFileName());
        values.put(LAST_VERIFIED_SHA256, result.getFileSha256());
        values.put(LAST_VERIFICATION_SUCCESS, String.valueOf(result.isSuccess()));
        values.put(LAST_VERIFICATION_MESSAGE, result.getMessage());
        save(session, values);
    }

    private static void save(IOSession session, Map<String, Object> values) {
        session.getResponseSync(ContentType.JSON, values, ActionType.SET_WEBSITE, Object.class);
    }
}
