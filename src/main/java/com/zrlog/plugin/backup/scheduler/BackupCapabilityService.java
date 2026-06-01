package com.zrlog.plugin.backup.scheduler;

import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.api.Capability;
import com.zrlog.plugin.api.IPluginService;
import com.zrlog.plugin.api.ScheduledCapability;
import com.zrlog.plugin.api.Service;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.message.CapabilityInvokeResult;

import java.util.HashMap;
import java.util.Map;

@Service(BackupCapabilityService.CAPABILITY_KEY)
@Capability(key = BackupCapabilityService.CAPABILITY_KEY, riskLevel = "medium")
@ScheduledCapability(
        key = BackupCapabilityService.CAPABILITY_KEY,
        label = "执行数据库备份",
        description = "导出 MySQL 备份文件；产生新文件时同步到私有存储。",
        defaultCron = BackupCapabilityService.DEFAULT_CRON,
        timeoutSeconds = 600
)
public class BackupCapabilityService implements IPluginService {

    public static final String CAPABILITY_KEY = "backupSqlFile.backup";
    public static final String DEFAULT_CRON = "0 2 * * *";

    @Override
    public void handle(IOSession session, MsgPacket msgPacket) {
        BackupRunResult runResult = new BackupJob(session).runBackup(true,
                "Scheduled backup completed successfully", "Backup error");
        CapabilityInvokeResult result = new CapabilityInvokeResult();
        result.setSuccess(runResult.isSuccess());
        if (!runResult.isSuccess()) {
            result.setErrorMessage(runResult.getMessage());
        }
        Map<String, Object> data = new HashMap<>();
        data.put("filesCount", runResult.getFilesCount());
        data.put("fileName", runResult.getFileName());
        data.put("newFile", runResult.isNewFile());
        data.put("message", runResult.getMessage());
        result.setData(data);
        session.sendJsonMsg(result, msgPacket.getMethodStr(), msgPacket.getMsgId(),
                result.isSuccess() ? MsgPacketStatus.RESPONSE_SUCCESS : MsgPacketStatus.RESPONSE_ERROR);
    }
}
