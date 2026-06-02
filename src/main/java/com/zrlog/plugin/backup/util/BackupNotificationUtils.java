package com.zrlog.plugin.backup.util;

import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.backup.model.BackupNotificationChannels;
import com.zrlog.plugin.backup.scheduler.BackupCapabilityService;
import com.zrlog.plugin.backup.scheduler.BackupRunResult;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.message.NotificationRequest;
import com.zrlog.plugin.render.SimpleTemplateRender;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BackupNotificationUtils {

    private static final Duration NOTIFICATION_TIMEOUT = Duration.ofSeconds(60);
    private static final SimpleTemplateRender TEMPLATE_RENDER = new SimpleTemplateRender();

    private BackupNotificationUtils() {
    }

    public static void publishBackupSuccess(IOSession session,
                                            BackupRunResult runResult,
                                            BackupNotificationChannels channels) {
        NotificationRequest request = createRequest(session, runResult,
                BackupNotificationChannels.normalize(channels).successChannels(),
                "backupSqlFile.backup.completed",
                "[ZrLog 数据库备份] " + titleMessage(runResult),
                "info",
                "/notification/backup-success");
        publish(session, request);
    }

    public static void publishBackupFailure(IOSession session,
                                            BackupRunResult runResult,
                                            BackupNotificationChannels channels) {
        NotificationRequest request = createRequest(session, runResult,
                BackupNotificationChannels.normalize(channels).failedChannels(),
                "backupSqlFile.backup.failed",
                "[ZrLog 数据库备份] 执行失败",
                "warning",
                "/notification/backup-failure");
        publish(session, request);
    }

    private static NotificationRequest createRequest(IOSession session,
                                                     BackupRunResult runResult,
                                                     java.util.List<String> channels,
                                                     String eventType,
                                                     String title,
                                                     String level,
                                                     String template) {
        NotificationRequest request = new NotificationRequest();
        request.setSourcePluginId(session.getPlugin().getId());
        request.setSourcePluginName(session.getPlugin().getShortName());
        request.setSourceCapabilityKey(BackupCapabilityService.CAPABILITY_KEY);
        request.setEventType(eventType);
        request.setNotificationType("backup");
        request.setChannels(channels);
        request.setTitle(title);
        request.setContent(TEMPLATE_RENDER.render(template, session.getPlugin(), templateData(runResult)));
        request.setLevel(level);
        request.setRequestId(UUID.randomUUID().toString());
        request.setPayload(payload(runResult));
        return request;
    }

    private static void publish(IOSession session, NotificationRequest request) {
        int msgId = session.publishNotification(request, null);
        MsgPacket response = session.getResponseMsgPacketByMsgId(msgId, NOTIFICATION_TIMEOUT);
        if (response == null) {
            throw new IllegalStateException("notification publish response timeout");
        }
        if (response.getStatus() != MsgPacketStatus.RESPONSE_SUCCESS) {
            throw new IllegalStateException("notification publish failed " + response.getStatus());
        }
    }

    private static String titleMessage(BackupRunResult runResult) {
        if (runResult == null || runResult.getMessage() == null || runResult.getMessage().trim().isEmpty()) {
            return "执行完成";
        }
        return runResult.getMessage();
    }

    private static Map<String, Object> templateData(BackupRunResult runResult) {
        Map<String, Object> map = new HashMap<>();
        map.put("fileName", escape(emptyText(runResult == null ? null : runResult.getFileName())));
        map.put("filesCount", runResult == null ? 0 : runResult.getFilesCount());
        map.put("newFileText", runResult != null && runResult.isNewFile() ? "已生成新文件" : "未检测到数据库变更");
        map.put("message", escape(emptyText(runResult == null ? null : runResult.getMessage())));
        return map;
    }

    private static Map<String, Object> payload(BackupRunResult runResult) {
        Map<String, Object> map = new HashMap<>();
        map.put("fileName", runResult == null ? "" : emptyText(runResult.getFileName()));
        map.put("filesCount", runResult == null ? 0 : runResult.getFilesCount());
        map.put("newFile", runResult != null && runResult.isNewFile());
        map.put("message", runResult == null ? "" : emptyText(runResult.getMessage()));
        return map;
    }

    private static String emptyText(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
