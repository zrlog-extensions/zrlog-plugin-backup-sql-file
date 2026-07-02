package com.zrlog.plugin.backup.service;

import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.backup.model.BackupNotificationChannels;
import com.zrlog.plugin.backup.model.BackupNotificationSettingValues;
import com.zrlog.plugin.backup.model.WebsiteKeyRequest;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.common.SessionKvRepository;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.type.ActionType;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BackupNotificationSettingRepository {

    private static final Logger LOGGER = LoggerUtil.getLogger(BackupNotificationSettingRepository.class);
    private static final BackupNotificationSettingRepository INSTANCE = new BackupNotificationSettingRepository();

    public static BackupNotificationSettingRepository getInstance() {
        return INSTANCE;
    }

    public BackupNotificationChannels get(IOSession session) {
        try {
            BackupNotificationSettingValues values = session.getResponseSync(ContentType.JSON,
                    WebsiteKeyRequest.of(BackupNotificationChannels.SUCCESS_CHANNELS_KEY + ","
                            + BackupNotificationChannels.FAILED_CHANNELS_KEY),
                    ActionType.GET_WEBSITE, BackupNotificationSettingValues.class);
            if (values == null) {
                values = new BackupNotificationSettingValues();
            }
            BackupNotificationChannels channels = new BackupNotificationChannels();
            channels.setSuccessChannels(BackupNotificationChannels.decodeChannels(
                    values.getNotificationSuccessChannels(), null));
            channels.setFailedChannels(BackupNotificationChannels.decodeChannels(
                    values.getNotificationFailedChannels(),
                    channels.getSuccessChannels()));
            return BackupNotificationChannels.normalize(channels);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "read backup notification channels from website config error", e);
            return BackupNotificationChannels.defaults();
        }
    }

    public void save(IOSession session, BackupNotificationChannels channels) {
        BackupNotificationChannels normalized = BackupNotificationChannels.normalize(channels);
        Map<String, String> values = new HashMap<>();
        values.put(BackupNotificationChannels.SUCCESS_CHANNELS_KEY,
                BackupNotificationChannels.encodeChannels(normalized.getSuccessChannels()));
        values.put(BackupNotificationChannels.FAILED_CHANNELS_KEY,
                BackupNotificationChannels.encodeChannels(normalized.getFailedChannels()));
        SessionKvRepository.of(session).write(values);
    }

}
