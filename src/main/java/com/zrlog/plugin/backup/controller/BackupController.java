package com.zrlog.plugin.backup.controller;

import com.google.gson.Gson;
import com.zrlog.plugin.IMsgPacketCallBack;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.backup.Start;
import com.zrlog.plugin.backup.scheduler.BackupJob;
import com.zrlog.plugin.common.IdUtil;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.data.codec.HttpRequestInfo;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.type.ActionType;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by xiaochun on 2016/2/13.
 */
public class BackupController {

    private static Logger LOGGER = Logger.getLogger(BackupController.class);

    private IOSession session;
    private MsgPacket requestPacket;
    private HttpRequestInfo requestInfo;

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
        session.sendMsg(new MsgPacket(requestInfo.simpleParam(), ContentType.JSON, MsgPacketStatus.SEND_REQUEST, IdUtil.getInt(), ActionType.SET_WEBSITE.name()), new IMsgPacketCallBack() {
            @Override
            public void handler(MsgPacket msgPacket) {
                Map<String, Object> map = new HashMap<>();
                map.put("success", true);
                session.sendMsg(new MsgPacket(map, ContentType.JSON, MsgPacketStatus.RESPONSE_SUCCESS, requestPacket.getMsgId(), requestPacket.getMethodStr()));
            }
        });
    }

    public void exportSqlFile() {
        session.sendJsonMsg(new HashMap<>(), ActionType.GET_DB_PROPERTIES.name(), IdUtil.getInt(), MsgPacketStatus.SEND_REQUEST, new IMsgPacketCallBack() {
            @Override
            public void handler(final MsgPacket response) {
                Map<String, Object> map = new Gson().fromJson(response.getDataStr(), Map.class);
                Properties properties = new Properties();
                try {
                    properties.load(new FileInputStream((String) map.get("dbProperties")));
                    File file = BackupJob.backupThenStoreToPrivateStore(session, properties);
                    if (file.exists()) {
                        session.sendFileMsg(file, requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_SUCCESS);
                    } else {
                        session.sendFileMsg(file, requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_ERROR);
                    }
                } catch (Exception e) {
                    LOGGER.error("", e);
                }

            }
        });

    }

    public void index() {
        Map<String, Object> keyMap = new HashMap<>();
        keyMap.put("key", "cycle");
        session.sendJsonMsg(keyMap, ActionType.GET_WEBSITE.name(), IdUtil.getInt(), MsgPacketStatus.SEND_REQUEST, new IMsgPacketCallBack() {
            @Override
            public void handler(MsgPacket msgPacket) {
                Map map = new Gson().fromJson(msgPacket.getDataStr(), Map.class);
                if (map.get("cycle") == null) {
                    map.put("cycle", "3600");
                }
                session.responseHtml("/templates/index.ftl", map, requestPacket.getMethodStr(), requestPacket.getMsgId());
            }
        });

    }

    public void filelist() {
        File[] files = new File(Start.sqlPath).listFiles();
        List<File> fileList = new ArrayList<>();
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.isFile()) {
                    fileList.add(file);
                }
            }
            Collections.sort(fileList, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    return (int) (f2.lastModified() - f1.lastModified());
                }
            });
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
        map.put("maxKeepSize", Start.maxBackupSqlFileCount);
        session.responseHtml("/templates/filelist.ftl", map, requestPacket.getMethodStr(), requestPacket.getMsgId());
    }

    public void downfile() {
        File file = new File(Start.sqlPath + requestInfo.simpleParam().get("file"));
        if (file.exists()) {
            session.sendFileMsg(file, requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_SUCCESS);
        } else {
            session.sendFileMsg(file, requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_ERROR);
        }
    }
}
