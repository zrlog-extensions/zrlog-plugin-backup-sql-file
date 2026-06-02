package com.zrlog.plugin.backup.controller;

import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.backup.Application;
import com.zrlog.plugin.backup.model.BackupNotificationChannels;
import com.zrlog.plugin.backup.scheduler.BackupCapabilityService;
import com.zrlog.plugin.backup.scheduler.BackupJob;
import com.zrlog.plugin.backup.scheduler.BackupRunResult;
import com.zrlog.plugin.backup.service.BackupNotificationSettingRepository;
import com.zrlog.plugin.backup.util.FileUtils;
import com.zrlog.plugin.common.IdUtil;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.common.SessionNotificationChannelRepository;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.data.codec.HttpRequestInfo;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.message.NotificationChannelProvider;
import com.zrlog.plugin.message.NotificationChannelQueryResult;
import com.zrlog.plugin.message.SchedulerQueryResult;
import com.zrlog.plugin.type.ActionType;

import java.io.File;
import java.time.Duration;
import java.time.ZoneId;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by xiaochun on 2016/2/13.
 */
public class BackupController {

    private static final Logger LOGGER = LoggerUtil.getLogger(BackupController.class);

    private final IOSession session;
    private final MsgPacket requestPacket;
    private final HttpRequestInfo requestInfo;
    private final BackupNotificationSettingRepository notificationSettingRepository =
            BackupNotificationSettingRepository.getInstance();
    private final Gson gson = new Gson();

    public BackupController(IOSession session, MsgPacket requestPacket, HttpRequestInfo requestInfo) {
        this.session = session;
        this.requestPacket = requestPacket;
        this.requestInfo = requestInfo;
    }

    private static String formatFileSize(long fileS) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString;
        if (fileS < 1024L) {
            fileSizeString = df.format((double) fileS) + "B";
        } else if (fileS < 1048576L) {
            fileSizeString = df.format((double) fileS / 1024.0D) + "K";
        } else if (fileS < 1073741824L) {
            fileSizeString = df.format((double) fileS / 1048576.0D) + "M";
        } else {
            fileSizeString = df.format((double) fileS / 1.073741824E9D) + "G";
        }

        return fileSizeString;
    }

    public void update() {
        Map<String, Object> params = new HashMap<>(requestInfo.simpleParam());
        params.remove("backupCron");
        params.remove("cycle");
        session.sendMsg(new MsgPacket(params, ContentType.JSON, MsgPacketStatus.SEND_REQUEST, IdUtil.getInt(), ActionType.SET_WEBSITE.name()), msgPacket -> {
            if (msgPacket.getStatus() != MsgPacketStatus.RESPONSE_SUCCESS) {
                response(errorMap("配置保存失败"));
                return;
            }
            response(successMap(params));
        });
    }

    public void exportSqlFile() {
        try {
            File file = new BackupJob(session).backup(Application.sqlPath, null).getFile();
            if (file.exists()) {
                try {
                    session.sendFileMsg(file, requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_SUCCESS);
                } finally {
                    file.delete();
                }
            } else {
                session.sendFileMsg(file, requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_ERROR);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "", e);
        }
    }

    public void index() {
        Map<String, Object> data = new HashMap<>();
        data.put("theme", isDarkMode() ? "dark" : "light");
        data.put("data", new Gson().toJson(pageData()));
        session.responseHtml("/templates/index", data, requestPacket.getMethodStr(), requestPacket.getMsgId());
    }

    public void json() {
        response(pageData());
    }

    public void files() {
        File[] files = new File(getBackupFilePath()).listFiles();
        List<File> fileList = new ArrayList<>();
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.isFile() && BackupJob.isSqlFile(file)) {
                    fileList.add(file);
                }
            }
            fileList.sort((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
        }

        List<Map<String, Object>> fileListMap = new ArrayList<>();
        for (File file : fileList) {
            Map<String, Object> tMap = new HashMap<>();
            tMap.put("fileName", file.getName());
            tMap.put("index", fileList.indexOf(file) + 1);
            tMap.put("size", formatFileSize(file.length()));
            tMap.put("lastModified", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(file.lastModified())));
            fileListMap.add(tMap);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("files", fileListMap);
        data.put("maxKeepSize", Application.maxBackupSqlFileCount);
        response(successMap(data));
    }

    public void history() {
        Map<String, Object> keyMap = new HashMap<>();
        keyMap.put("key", "syncHistory");
        session.sendJsonMsg(keyMap, ActionType.GET_WEBSITE.name(), IdUtil.getInt(), MsgPacketStatus.SEND_REQUEST, msgPacket -> {
            Map responseMap = new Gson().fromJson(msgPacket.getDataStr(), Map.class);
            String syncHistoryJson = responseMap != null ? (String) responseMap.get("syncHistory") : null;
            List historyList = null;
            if (syncHistoryJson != null && !syncHistoryJson.trim().isEmpty()) {
                try {
                    historyList = new Gson().fromJson(syncHistoryJson, List.class);
                } catch (Exception e) {
                    historyList = new ArrayList<>();
                }
            }
            if (historyList == null) {
                historyList = new ArrayList<>();
            }
            response(successMap(historyList));
        });
    }

    public void backupNow() {
        BackupRunResult result = new BackupJob(session).runBackup(false,
                "Manual backup completed successfully", "Manual backup failed");
        if (result.isSuccess()) {
            response(successMap(result));
        } else {
            response(errorMap("手动备份失败: " + result.getMessage()));
        }
    }

    public void notificationChannels() {
        try {
            response(successMap(notificationChannelInfo()));
        } catch (Exception e) {
            response(errorMap(e.getMessage()));
        }
    }

    public void saveNotificationChannels() {
        Map<String, Object> params = requestInfo.simpleParam();
        List<NotificationChannelProvider> providers;
        try {
            providers = queryNotificationProviders();
        } catch (Exception e) {
            response(errorMap(e.getMessage()));
            return;
        }
        Set<String> availableChannels = availableChannels(providers);
        List<String> successChannels = configuredChannels(params.get("successChannels"), availableChannels);
        if (successChannels.isEmpty()) {
            response(errorMap("请选择 plugin-core 中可用的通知渠道"));
            return;
        }
        List<String> failedChannels = configuredChannels(params.get("failedChannels"), availableChannels);
        if (failedChannels.isEmpty()) {
            failedChannels = successChannels;
        }
        BackupNotificationChannels channels = new BackupNotificationChannels();
        channels.setSuccessChannels(successChannels);
        channels.setFailedChannels(failedChannels);
        notificationSettingRepository.save(session, channels);
        Map<String, Object> result = new HashMap<>();
        result.put("settings", notificationSettingRepository.get(session));
        result.put("providers", providers);
        response(successMap(result));
    }

    private String getBackupFilePath() {
        Map<String, Object> keyMap = new HashMap<>();
        keyMap.put("key", "backupFilePath");
        Map<String, String> responseMap = session.getResponseSync(ContentType.JSON, keyMap, ActionType.GET_WEBSITE, Map.class);
        String configPath = responseMap != null ? responseMap.get("backupFilePath") : null;
        return getBackupFilePath(configPath);
    }

    private String getBackupFilePath(String configPath) {
        if (Objects.isNull(configPath) || configPath.trim().isEmpty()) {
            return Application.sqlPath;
        }
        return configPath;
    }

    public void downfile() {
        File file = FileUtils.safeAppendFilePath(getBackupFilePath(), (String) requestInfo.simpleParam().get("file"));
        if (BackupJob.isSqlFile(file) && file.exists()) {
            session.sendFileMsg(file, requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_SUCCESS);
        } else {
            session.sendFileMsg(file, requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_ERROR);
        }
    }

    private Map<String, Object> pageData() {
        Map<String, Object> keyMap = new HashMap<>();
        keyMap.put("key", "backupPassword,backupFilePath");
        Map<String, String> getMap = session.getResponseSync(ContentType.JSON, keyMap, ActionType.GET_WEBSITE, Map.class);
        if (getMap == null) {
            getMap = new HashMap<>();
        }
        Map<String, Object> schedule = queryScheduleInfo();
        String backupCron = stringValue(schedule.get("cron"));
        if (!notBlank(backupCron)) {
            backupCron = BackupCapabilityService.DEFAULT_CRON;
        }
        getMap.put("backupCron", backupCron);
        getMap.put("cycle", cronToLegacyCycle(backupCron));
        getMap.putIfAbsent("backupPassword", "");
        getMap.putIfAbsent("backupFilePath", "");

        // Fetch backup files
        File[] files = new File(getBackupFilePath(getMap.get("backupFilePath"))).listFiles();
        List<File> fileList = new ArrayList<>();
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.isFile() && BackupJob.isSqlFile(file)) {
                    fileList.add(file);
                }
            }
            fileList.sort((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
        }

        List<Map<String, Object>> fileListMap = new ArrayList<>();
        for (File file : fileList) {
            Map<String, Object> tMap = new HashMap<>();
            tMap.put("fileName", file.getName());
            tMap.put("index", fileList.indexOf(file) + 1);
            tMap.put("size", formatFileSize(file.length()));
            tMap.put("lastModified", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(file.lastModified())));
            fileListMap.add(tMap);
        }

        // Fetch backup history
        Map<String, Object> historyKeyMap = new HashMap<>();
        historyKeyMap.put("key", "syncHistory");
        Map historyResponseMap = session.getResponseSync(ContentType.JSON, historyKeyMap, ActionType.GET_WEBSITE, Map.class);
        String syncHistoryJson = historyResponseMap != null ? (String) historyResponseMap.get("syncHistory") : null;
        List historyList = null;
        if (syncHistoryJson != null && !syncHistoryJson.trim().isEmpty()) {
            try {
                historyList = new Gson().fromJson(syncHistoryJson, List.class);
            } catch (Exception e) {
                historyList = new ArrayList<>();
            }
        }
        if (historyList == null) {
            historyList = new ArrayList<>();
        }
        boolean dark = requestInfo.isDarkMode();
        String colorPrimary = requestInfo.getAdminColorPrimary();

        Map<String, Object> data = new HashMap<>();
        data.put("dark", dark);
        data.put("colorPrimary", colorPrimary);
        data.put("plugin", session.getPlugin());
        data.put("config", getMap);
        data.put("files", fileListMap);
        data.put("history", historyList);
        data.put("maxKeepSize", Application.maxBackupSqlFileCount);
        data.put("schedulerTimezone", schedulerTimezone(schedule));
        data.put("schedule", schedule);
        data.put("notificationChannels", notificationSettingRepository.get(session));

        return successMap(data);
    }

    private Map<String, Object> queryScheduleInfo() {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Map<String, Object>> resultRef = new AtomicReference<>();
        try {
            session.querySchedule(BackupCapabilityService.CAPABILITY_KEY, msgPacket -> {
                try {
                    if (msgPacket.getStatus() != MsgPacketStatus.RESPONSE_SUCCESS) {
                        resultRef.set(scheduleError("调度信息查询失败"));
                        return;
                    }
                    SchedulerQueryResult result = gson.fromJson(msgPacket.getDataStr(), SchedulerQueryResult.class);
                    if (result == null) {
                        resultRef.set(scheduleError("调度信息为空"));
                    } else if (!result.isSuccess()) {
                        resultRef.set(scheduleError(notBlank(result.getErrorMessage()) ? result.getErrorMessage() : "调度信息查询失败"));
                    } else {
                        resultRef.set(scheduleMap(result));
                    }
                } finally {
                    latch.countDown();
                }
            });
            if (!latch.await(15, TimeUnit.SECONDS)) {
                return scheduleError("调度信息查询超时");
            }
            Map<String, Object> result = resultRef.get();
            return result == null ? scheduleError("调度信息查询失败") : result;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "query backup schedule error", e);
            return scheduleError(e.getMessage());
        }
    }

    private Map<String, Object> scheduleMap(SchedulerQueryResult result) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        map.put("automationId", result.getAutomationId());
        map.put("capabilityKey", result.getCapabilityKey());
        map.put("name", result.getName());
        map.put("cron", result.getCron());
        map.put("timezone", result.getTimezone());
        map.put("enabled", result.getEnabled());
        map.put("nextRunAt", result.getNextRunAt());
        map.put("lastRunAt", result.getLastRunAt());
        return map;
    }

    private Map<String, Object> scheduleError(String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", false);
        map.put("capabilityKey", BackupCapabilityService.CAPABILITY_KEY);
        map.put("cron", BackupCapabilityService.DEFAULT_CRON);
        map.put("timezone", ZoneId.systemDefault().toString());
        map.put("enabled", false);
        map.put("errorMessage", notBlank(message) ? message : "调度信息查询失败");
        return map;
    }

    private String schedulerTimezone(Map<String, Object> schedule) {
        String timezone = stringValue(schedule.get("timezone"));
        return notBlank(timezone) ? timezone : ZoneId.systemDefault().toString();
    }

    private Map<String, Object> notificationChannelInfo() {
        Map<String, Object> data = new HashMap<>();
        data.put("settings", notificationSettingRepository.get(session));
        data.put("providers", queryNotificationProviders());
        return data;
    }

    private List<NotificationChannelProvider> queryNotificationProviders() {
        NotificationChannelQueryResult result = SessionNotificationChannelRepository.of(session).query(Duration.ofSeconds(15));
        if (!result.isOk()) {
            String message = stringValue(result.getMessage());
            throw new IllegalStateException(notBlank(message) ? message : "通知渠道查询失败");
        }
        return result.getItems();
    }

    private Set<String> availableChannels(List<NotificationChannelProvider> providers) {
        Set<String> channels = new LinkedHashSet<>();
        for (NotificationChannelProvider item : providers) {
            String channel = item == null ? "" : item.getChannel();
            if (notBlank(channel)) {
                channels.add(channel);
            }
        }
        return channels;
    }

    private List<String> configuredChannels(Object value, Set<String> availableChannels) {
        List<String> result = new ArrayList<>();
        for (String channel : channelList(value)) {
            if (availableChannels.contains(channel) && !result.contains(channel)) {
                result.add(channel);
            }
        }
        return result;
    }

    private boolean isDarkMode() {
        return requestInfo.isDarkMode();
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String stringValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof List && !((List) value).isEmpty()) {
            return String.valueOf(((List) value).get(0));
        }
        return String.valueOf(value).trim();
    }

    private List<String> channelList(Object value) {
        if (value instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List) value) {
                addChannels(result, stringValue(item));
            }
            return result;
        }
        return Arrays.asList(stringValue(value).split(","));
    }

    private void addChannels(List<String> result, String text) {
        if (!notBlank(text)) {
            return;
        }
        String[] values = text.split(",");
        for (String value : values) {
            if (notBlank(value)) {
                result.add(value.trim());
            }
        }
    }

    private String cronToLegacyCycle(String cron) {
        if ("*/1 * * * *".equals(cron) || "* * * * *".equals(cron)) {
            return "60";
        }
        if ("0 * * * *".equals(cron)) {
            return "3600";
        }
        if ("0 */6 * * *".equals(cron)) {
            return "21600";
        }
        if ("0 */12 * * *".equals(cron)) {
            return "43200";
        }
        return "86400";
    }

    private Map<String, Object> successMap(Object data) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        map.put("data", data);
        return map;
    }

    private Map<String, Object> errorMap(String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", false);
        map.put("message", message);
        return map;
    }

    private void response(Map<String, Object> map) {
        session.sendMsg(new MsgPacket(map, ContentType.JSON, MsgPacketStatus.RESPONSE_SUCCESS, requestPacket.getMsgId(), requestPacket.getMethodStr()));
    }
}
