package com.zrlog.plugin.backup.service;

import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.backup.model.BackupNotificationChannels;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.common.SessionKvRepository;

import java.util.logging.Level;
import java.util.logging.Logger;

public class BackupNotificationSettingRepository {

    private static final Logger LOGGER = LoggerUtil.getLogger(BackupNotificationSettingRepository.class);
    private static final BackupNotificationSettingRepository INSTANCE = new BackupNotificationSettingRepository();
    private final Gson gson = new Gson();

    public static BackupNotificationSettingRepository getInstance() {
        return INSTANCE;
    }

    public BackupNotificationChannels get(IOSession session) {
        try {
            String json = SessionKvRepository.of(session).get(BackupNotificationChannels.STORE_KEY).orElse("");
            if (!notBlank(json)) {
                return BackupNotificationChannels.defaults();
            }
            return BackupNotificationChannels.normalize(gson.fromJson(json, BackupNotificationChannels.class));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "read backup notification channels from website config error", e);
            return BackupNotificationChannels.defaults();
        }
    }

    public void save(IOSession session, BackupNotificationChannels channels) {
        SessionKvRepository.of(session).put(BackupNotificationChannels.STORE_KEY,
                gson.toJson(BackupNotificationChannels.normalize(channels)));
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
