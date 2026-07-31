package com.zrlog.plugin.backup.scheduler;

import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.api.Capability;
import com.zrlog.plugin.api.IPluginService;
import com.zrlog.plugin.api.ScheduledCapability;
import com.zrlog.plugin.api.Service;
import com.zrlog.plugin.backup.model.RestoreDrillResult;
import com.zrlog.plugin.backup.model.BackupNotificationChannels;
import com.zrlog.plugin.backup.service.BackupNotificationSettingRepository;
import com.zrlog.plugin.backup.service.RestoreDrillService;
import com.zrlog.plugin.backup.util.BackupNotificationUtils;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.message.CapabilityInvokeResult;

import java.util.LinkedHashMap;
import java.util.Map;

@Service(RestoreDrillCapabilityService.CAPABILITY_KEY)
@Capability(key = RestoreDrillCapabilityService.CAPABILITY_KEY, riskLevel = "high")
@ScheduledCapability(
        key = RestoreDrillCapabilityService.CAPABILITY_KEY,
        label = "验证数据库备份可恢复性",
        description = "把最新备份恢复到同一 MySQL 服务的随机临时数据库，验证核心表后立即删除。",
        defaultCron = RestoreDrillCapabilityService.DEFAULT_CRON,
        timeoutSeconds = 1800
)
public class RestoreDrillCapabilityService implements IPluginService {

    public static final String CAPABILITY_KEY = "backupSqlFile.restoreDrill";
    public static final String DEFAULT_CRON = "0 3 * * 0";

    @Override
    public void handle(IOSession session, MsgPacket msgPacket) {
        BackupNotificationChannels channels = BackupNotificationSettingRepository.getInstance().get(session);
        RestoreDrillResult drillResult = new RestoreDrillService().run(session);
        CapabilityInvokeResult result = new CapabilityInvokeResult();
        result.setSuccess(drillResult.isSuccess());
        if (!drillResult.isSuccess()) {
            result.setErrorMessage(drillResult.getMessage());
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("startedAt", drillResult.getStartedAt());
        data.put("completedAt", drillResult.getCompletedAt());
        data.put("fileName", drillResult.getFileName());
        data.put("fileSha256", drillResult.getFileSha256());
        data.put("restoredTableCount", drillResult.getRestoredTableCount());
        data.put("restoredCoreRowCount", drillResult.getRestoredCoreRowCount());
        data.put("message", drillResult.getMessage());
        try {
            if (drillResult.isSuccess()) {
                BackupNotificationUtils.publishRestoreDrillSuccess(session, drillResult, channels);
            } else {
                BackupNotificationUtils.publishRestoreDrillFailure(session, drillResult, channels);
            }
            data.put("notificationSuccess", true);
            data.put("notificationChannels", drillResult.isSuccess()
                    ? channels.successChannels() : channels.failedChannels());
        } catch (Exception notificationError) {
            data.put("notificationSuccess", false);
            data.put("notificationError", notificationError.getMessage());
            data.put("notificationChannels", drillResult.isSuccess()
                    ? channels.successChannels() : channels.failedChannels());
        }
        result.setData(data);
        session.sendJsonMsg(result, msgPacket.getMethodStr(), msgPacket.getMsgId(),
                result.isSuccess() ? MsgPacketStatus.RESPONSE_SUCCESS : MsgPacketStatus.RESPONSE_ERROR);
    }
}
