package com.zrlog.plugin.backup.controller;

import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.backup.Application;
import com.zrlog.plugin.backup.scheduler.BackupJob;
import com.zrlog.plugin.backup.util.FileUtils;
import com.zrlog.plugin.common.IdUtil;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.common.model.PublicInfo;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.data.codec.HttpRequestInfo;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.type.ActionType;

import java.nio.charset.StandardCharsets;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
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
        session.sendMsg(new MsgPacket(requestInfo.simpleParam(), ContentType.JSON, MsgPacketStatus.SEND_REQUEST, IdUtil.getInt(), ActionType.SET_WEBSITE.name()), msgPacket -> {
            Application.backupConnectHandle.refresh(session);
            response(successMap(null));
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
        try {
            String backupFilePath = getBackupFilePath();
            Map<String, Object> keyMap = new HashMap<>();
            keyMap.put("key", "backupPassword");
            session.sendJsonMsg(keyMap, ActionType.GET_WEBSITE.name(), IdUtil.getInt(), MsgPacketStatus.SEND_REQUEST, msgPacket -> {
                Map responseMap = new Gson().fromJson(msgPacket.getDataStr(), Map.class);
                String backupPassword = responseMap != null ? (String) responseMap.get("backupPassword") : null;

                try {
                    BackupJob backupJob = new BackupJob(session);
                    com.zrlog.plugin.backup.scheduler.BackupResultVO resultVO = backupJob.backup(backupFilePath, backupPassword);
                    File file = resultVO.getFile();
                    if (file == null || !file.exists() || file.length() == 0) {
                        throw new RuntimeException("备份文件未生成或文件大小为0");
                    }

                    File[] files = new File(backupFilePath).listFiles();
                    int count = 0;
                    if (files != null) {
                        for (File f : files) {
                            if (f.isFile() && BackupJob.isSqlFile(f)) {
                                count++;
                            }
                        }
                    }
                    backupJob.recordBackupHistory(true, count, "Manual backup completed successfully");
                    response(successMap(null));
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Manual backup failed", e);
                    new BackupJob(session).recordBackupHistory(false, 0, "Manual backup failed: " + e.getMessage());

                    Map<String, Object> map = new HashMap<>();
                    map.put("success", false);
                    map.put("message", "手动备份失败: " + e.getMessage());
                    response(map);
                }
            });
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Manual backup failed before running", e);
            new BackupJob(session).recordBackupHistory(false, 0, "Manual backup failed before running: " + e.getMessage());
            Map<String, Object> map = new HashMap<>();
            map.put("success", false);
            map.put("message", "手动备份失败: " + e.getMessage());
            response(map);
        }
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
        keyMap.put("key", "cycle,backupPassword,backupFilePath");
        Map<String, String> getMap = session.getResponseSync(ContentType.JSON, keyMap, ActionType.GET_WEBSITE, Map.class);
        if (getMap == null) {
            getMap = new HashMap<>();
        }
        getMap.putIfAbsent("cycle", "3600");
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

        // Load public info (theme, primary color)
        PublicInfo publicInfo = publicInfo();
        boolean dark = publicInfo.getDarkMode() == null ? isDarkMode() : publicInfo.getDarkMode();
        String colorPrimary = "#1677ff";
        if (requestInfo.getHeader() != null && notBlank(requestInfo.getHeader().get("Admin-Color-Primary"))) {
            colorPrimary = requestInfo.getHeader().get("Admin-Color-Primary");
        } else if (publicInfo != null && notBlank(publicInfo.getAdminColorPrimary())) {
            colorPrimary = publicInfo.getAdminColorPrimary();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("dark", dark);
        data.put("colorPrimary", colorPrimary);
        data.put("plugin", session.getPlugin());
        data.put("config", getMap);
        data.put("files", fileListMap);
        data.put("history", historyList);
        data.put("maxKeepSize", Application.maxBackupSqlFileCount);

        return successMap(data);
    }

    private PublicInfo publicInfo() {
        try {
            PublicInfo publicInfo = session.getResponseSync(ContentType.JSON, new HashMap<>(), ActionType.LOAD_PUBLIC_INFO, PublicInfo.class);
            return publicInfo == null ? new PublicInfo() : publicInfo;
        } catch (Exception e) {
            return new PublicInfo();
        }
    }

    private boolean isDarkMode() {
        return requestInfo.getHeader() != null && Objects.equals(requestInfo.getHeader().get("Dark-Mode"), "true");
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private Map<String, Object> successMap(Object data) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        map.put("data", data);
        return map;
    }

    private void response(Map<String, Object> map) {
        session.sendMsg(new MsgPacket(map, ContentType.JSON, MsgPacketStatus.RESPONSE_SUCCESS, requestPacket.getMsgId(), requestPacket.getMethodStr()));
    }
}
