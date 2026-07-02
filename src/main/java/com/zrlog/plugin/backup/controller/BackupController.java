package com.zrlog.plugin.backup.controller;

import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.backup.Application;
import com.zrlog.plugin.backup.model.BackupApiResponse;
import com.zrlog.plugin.backup.model.BackupConfig;
import com.zrlog.plugin.backup.model.BackupConfigValues;
import com.zrlog.plugin.backup.model.BackupFileRecord;
import com.zrlog.plugin.backup.model.BackupNotificationChannelInfo;
import com.zrlog.plugin.backup.model.BackupNotificationChannels;
import com.zrlog.plugin.backup.model.BackupPageData;
import com.zrlog.plugin.backup.model.BackupRequestParams;
import com.zrlog.plugin.backup.model.SiteExportPreviewResponse;
import com.zrlog.plugin.backup.model.WebsiteKeyRequest;
import com.zrlog.plugin.backup.scheduler.BackupCapabilityService;
import com.zrlog.plugin.backup.scheduler.BackupJob;
import com.zrlog.plugin.backup.scheduler.BackupRunResult;
import com.zrlog.plugin.backup.service.BackupNotificationSettingRepository;
import com.zrlog.plugin.backup.service.SiteExportService;
import com.zrlog.plugin.backup.service.SiteImportPrecheckService;
import com.zrlog.plugin.client.HttpClientUtils;
import com.zrlog.plugin.common.IOUtil;
import com.zrlog.plugin.backup.util.FileUtils;
import com.zrlog.plugin.common.IdUtil;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.common.PathKit;
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
        BackupConfig config = params().toConfig();
        BackupConfigValues request = new BackupConfigValues();
        request.setBackupPassword(config.getBackupPassword());
        request.setBackupFilePath(config.getBackupFilePath());
        session.sendMsg(new MsgPacket(request, ContentType.JSON, MsgPacketStatus.SEND_REQUEST, IdUtil.getInt(), ActionType.SET_WEBSITE.name()), msgPacket -> {
            if (msgPacket.getStatus() != MsgPacketStatus.RESPONSE_SUCCESS) {
                response(BackupApiResponse.error("配置保存失败"));
                return;
            }
            response(BackupApiResponse.success(config));
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
        index();
    }

    public void history() {
        session.sendJsonMsg(WebsiteKeyRequest.of("syncHistory"), ActionType.GET_WEBSITE.name(), IdUtil.getInt(),
                MsgPacketStatus.SEND_REQUEST, msgPacket -> {
            BackupConfigValues values = gson.fromJson(msgPacket.getDataStr(), BackupConfigValues.class);
            response(BackupApiResponse.success(historyList(values == null ? null : values.getSyncHistory())));
        });
    }

    public void backupNow() {
        BackupRunResult result = new BackupJob(session).runBackup(false,
                "Manual backup completed successfully", "Manual backup failed");
        if (result.isSuccess()) {
            response(BackupApiResponse.success(result));
        } else {
            response(BackupApiResponse.error("手动备份失败: " + result.getMessage()));
        }
    }

    public void siteExportPreview() {
        try {
            response(BackupApiResponse.success(new SiteExportService(session).preview(parseSiteExportOptions())));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Site export preview failed", e);
            response(BackupApiResponse.error("全站导出预览失败: " + e.getMessage()));
        }
    }

    public void siteExportDownload() {
        SiteExportService.SiteExportPackage exportPackage = null;
        try {
            exportPackage = new SiteExportService(session).createPackage(parseSiteExportOptions());
            File file = exportPackage.getFile();
            if (file.exists()) {
                session.sendFileMsg(file, requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_SUCCESS);
            } else {
                session.sendFileMsg(file, requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_ERROR);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Site export download failed", e);
            response(BackupApiResponse.error("全站导出失败: " + e.getMessage()));
        } finally {
            if (exportPackage != null && exportPackage.getFile() != null && exportPackage.getFile().exists()) {
                deleteQuietly(exportPackage.getFile().getParentFile());
            }
        }
    }

    public void siteImportPreview() {
        String source = params().getSource();
        if (!notBlank(source)) {
            response(BackupApiResponse.error("请先上传全站导出 zip 文件"));
            return;
        }
        File tmpPath = new File(PathKit.getTmpPath() + "/" + UUID.randomUUID() + "/");
        File zipFile = new File(tmpPath, "site-export.zip");
        try {
            Map<String, String> requestHeaders = new HashMap<>();
            if (requestInfo.getHeader() != null && requestInfo.getHeader().get("Cookie") != null) {
                requestHeaders.put("Cookie", requestInfo.getHeader().get("Cookie"));
            }
            byte[] bytes = HttpClientUtils.sendGetRequest(source, byte[].class, requestHeaders, session, Duration.ofSeconds(360));
            IOUtil.writeBytesToFile(bytes, zipFile);
            response(BackupApiResponse.success(new SiteImportPrecheckService(session).precheck(zipFile)));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Site import precheck failed", e);
            response(BackupApiResponse.error("导入前预检失败: " + e.getMessage()));
        } finally {
            deleteQuietly(tmpPath);
        }
    }

    public void notificationChannels() {
        try {
            response(BackupApiResponse.success(notificationChannelInfo()));
        } catch (Exception e) {
            response(BackupApiResponse.error(e.getMessage()));
        }
    }

    public void saveNotificationChannels() {
        BackupRequestParams params = params();
        List<NotificationChannelProvider> providers;
        try {
            providers = queryNotificationProviders();
        } catch (Exception e) {
            response(BackupApiResponse.error(e.getMessage()));
            return;
        }
        Set<String> availableChannels = availableChannels(providers);
        List<String> successChannels = configuredChannels(params.getSuccessChannels(), availableChannels);
        if (successChannels.isEmpty()) {
            response(BackupApiResponse.error("请选择 plugin-core 中可用的通知渠道"));
            return;
        }
        List<String> failedChannels = configuredChannels(params.getFailedChannels(), availableChannels);
        if (failedChannels.isEmpty()) {
            failedChannels = successChannels;
        }
        BackupNotificationChannels channels = new BackupNotificationChannels();
        channels.setSuccessChannels(successChannels);
        channels.setFailedChannels(failedChannels);
        notificationSettingRepository.save(session, channels);
        BackupNotificationChannelInfo result = new BackupNotificationChannelInfo();
        result.setSettings(notificationSettingRepository.get(session));
        result.setProviders(providers);
        response(BackupApiResponse.success(result));
    }

    private String getBackupFilePath() {
        BackupConfigValues response = session.getResponseSync(ContentType.JSON, WebsiteKeyRequest.of("backupFilePath"),
                ActionType.GET_WEBSITE, BackupConfigValues.class);
        String configPath = response != null ? response.getBackupFilePath() : null;
        return getBackupFilePath(configPath);
    }

    private String getBackupFilePath(String configPath) {
        if (Objects.isNull(configPath) || configPath.trim().isEmpty()) {
            return Application.sqlPath;
        }
        return configPath;
    }

    public void downfile() {
        File file = FileUtils.safeAppendFilePath(getBackupFilePath(), params().getFile());
        if (BackupJob.isSqlFile(file) && file.exists()) {
            session.sendFileMsg(file, requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_SUCCESS);
        } else {
            session.sendFileMsg(file, requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_ERROR);
        }
    }

    private BackupApiResponse<BackupPageData> pageData() {
        BackupConfigValues values = session.getResponseSync(ContentType.JSON,
                WebsiteKeyRequest.of("backupPassword,backupFilePath"), ActionType.GET_WEBSITE, BackupConfigValues.class);
        if (values == null) {
            values = new BackupConfigValues();
        }
        Map<String, Object> schedule = queryScheduleInfo();
        String backupCron = stringValue(schedule.get("cron"));
        if (!notBlank(backupCron)) {
            backupCron = BackupCapabilityService.DEFAULT_CRON;
        }
        BackupConfig config = new BackupConfig();
        config.setBackupPassword(defaultText(values.getBackupPassword(), ""));
        config.setBackupFilePath(defaultText(values.getBackupFilePath(), ""));
        config.setBackupCron(backupCron);
        config.setCycle(cronToLegacyCycle(backupCron));

        // Fetch backup files
        File[] files = new File(getBackupFilePath(config.getBackupFilePath())).listFiles();
        List<File> fileList = new ArrayList<>();
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.isFile() && BackupJob.isSqlFile(file)) {
                    fileList.add(file);
                }
            }
            fileList.sort((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
        }

        List<BackupFileRecord> fileListMap = new ArrayList<>();
        for (File file : fileList) {
            BackupFileRecord record = new BackupFileRecord();
            record.setFileName(file.getName());
            record.setIndex(fileList.indexOf(file) + 1);
            record.setSize(formatFileSize(file.length()));
            record.setLastModified(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(file.lastModified())));
            fileListMap.add(record);
        }

        // Fetch backup history
        BackupConfigValues historyResponse = session.getResponseSync(ContentType.JSON, WebsiteKeyRequest.of("syncHistory"),
                ActionType.GET_WEBSITE, BackupConfigValues.class);

        BackupPageData data = new BackupPageData();
        data.setDark(requestInfo.isDarkMode());
        data.setColorPrimary(requestInfo.getAdminColorPrimary());
        data.setPlugin(session.getPlugin());
        data.setConfig(config);
        data.setFiles(fileListMap);
        data.setHistory(historyList(historyResponse == null ? null : historyResponse.getSyncHistory()));
        data.setMaxKeepSize(Application.maxBackupSqlFileCount);
        data.setSchedulerTimezone(schedulerTimezone(schedule));
        data.setSchedule(schedule);
        data.setNotificationChannels(notificationSettingRepository.get(session));
        try {
            data.setSiteExport(new SiteExportService(session).preview(new SiteExportPreviewResponse.SiteExportOptions()));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "load site export preview error", e);
            data.setSiteExport(new SiteExportPreviewResponse());
            data.setSiteExportError(e.getMessage());
        }

        return BackupApiResponse.success(data);
    }

    private SiteExportPreviewResponse.SiteExportOptions parseSiteExportOptions() {
        return params().toSiteExportOptions();
    }

    private BackupRequestParams params() {
        return BackupRequestParams.fromParams(this::paramObject);
    }

    private Object paramObject(String key) {
        if (requestInfo.getParam() == null || requestInfo.getParam().get(key) == null || requestInfo.getParam().get(key).length == 0) {
            return null;
        }
        String[] values = requestInfo.getParam().get(key);
        return values.length == 1 ? values[0] : values;
    }

    private void deleteQuietly(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteQuietly(child);
                }
            }
        }
        file.delete();
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

    private BackupNotificationChannelInfo notificationChannelInfo() {
        BackupNotificationChannelInfo data = new BackupNotificationChannelInfo();
        data.setSettings(notificationSettingRepository.get(session));
        data.setProviders(queryNotificationProviders());
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
        return BackupRequestParams.channelList(value);
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

    private List historyList(String syncHistoryJson) {
        if (syncHistoryJson != null && !syncHistoryJson.trim().isEmpty()) {
            try {
                List historyList = gson.fromJson(syncHistoryJson, List.class);
                return historyList == null ? new ArrayList<>() : historyList;
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }

    private String defaultText(String value, String defaultValue) {
        return notBlank(value) ? value : defaultValue;
    }

    private void response(BackupApiResponse<?> response) {
        session.sendMsg(new MsgPacket(response, ContentType.JSON, MsgPacketStatus.RESPONSE_SUCCESS, requestPacket.getMsgId(), requestPacket.getMethodStr()));
    }
}
