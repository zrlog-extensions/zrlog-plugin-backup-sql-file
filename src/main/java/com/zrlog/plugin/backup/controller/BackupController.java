package com.zrlog.plugin.backup.controller;

import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.backup.Application;
import com.zrlog.plugin.backup.scheduler.BackupJob;
import com.zrlog.plugin.backup.util.FileUtils;
import com.zrlog.plugin.common.IdUtil;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.data.codec.HttpRequestInfo;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.type.ActionType;

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
            Map<String, Object> map = new HashMap<>();
            map.put("success", true);
            session.sendMsg(new MsgPacket(map, ContentType.JSON, MsgPacketStatus.RESPONSE_SUCCESS, requestPacket.getMsgId(), requestPacket.getMethodStr()));
            Application.backupConnectHandle.refresh(session);
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
        Map<String, Object> keyMap = new HashMap<>();
        keyMap.put("key", "cycle,backupPassword,backupFilePath");
        session.sendJsonMsg(keyMap, ActionType.GET_WEBSITE.name(), IdUtil.getInt(), MsgPacketStatus.SEND_REQUEST, msgPacket -> {
            Map map = new Gson().fromJson(msgPacket.getDataStr(), Map.class);
            map.putIfAbsent("cycle", "3600");
            map.putIfAbsent("backupPassword", "");
            map.put("theme", Objects.equals(requestInfo.getHeader().get("Dark-Mode"), "true") ? "dark" : "light");
            session.responseHtml("/templates/index", map, requestPacket.getMethodStr(), requestPacket.getMsgId());
        });

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
            fileList.sort((f1, f2) -> (int) (f2.lastModified() - f1.lastModified()));
        }

        Map map = new HashMap();
        List<Map<String, Object>> fileListMap = new ArrayList<>();
        for (File file : fileList) {
            Map<String, Object> tMap = new HashMap<>();
            tMap.put("fileName", file.getName());
            tMap.put("index", fileList.indexOf(file) + 1);
            tMap.put("size", formatFileSize(file.length()));
            tMap.put("lastModified", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(file.lastModified())));
            fileListMap.add(tMap);
        }
        map.put("files", fileListMap);
        map.put("maxKeepSize", Application.maxBackupSqlFileCount);
        map.put("theme", Objects.equals(requestInfo.getHeader().get("Dark-Mode"), "true") ? "dark" : "light");
        session.responseHtml("/templates/files", map, requestPacket.getMethodStr(), requestPacket.getMsgId());
    }

    private String getBackupFilePath() {
        Map<String, Object> keyMap = new HashMap<>();
        keyMap.put("key", "backupFilePath");
        Map<String, String> responseMap = session.getResponseSync(ContentType.JSON, keyMap, ActionType.GET_WEBSITE, Map.class);
        String configPath = responseMap.get("backupFilePath");
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
}
