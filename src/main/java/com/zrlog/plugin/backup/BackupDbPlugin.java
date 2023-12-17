package com.zrlog.plugin.backup;

import com.google.gson.Gson;
import com.zrlog.plugin.IMsgPacketCallBack;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.api.IPluginAction;
import com.zrlog.plugin.backup.controller.BackupController;
import com.zrlog.plugin.backup.scheduler.BackupJob;
import com.zrlog.plugin.common.IdUtil;
import com.zrlog.plugin.data.codec.HttpRequestInfo;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.type.ActionType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BackupDbPlugin implements IPluginAction {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void start(final IOSession ioSession, MsgPacket msgPacket) {
        ioSession.sendJsonMsg(new HashMap<>(), ActionType.GET_DB_PROPERTIES.name(), IdUtil.getInt(), MsgPacketStatus.SEND_REQUEST, new IMsgPacketCallBack() {
            @Override
            public void handler(final MsgPacket response) {
                Map<String, Object> keyMap = new HashMap<>();
                keyMap.put("key", "cycle");
                ioSession.sendJsonMsg(keyMap, ActionType.GET_WEBSITE.name(), IdUtil.getInt(), MsgPacketStatus.SEND_REQUEST, new IMsgPacketCallBack() {
                    @Override
                    public void handler(MsgPacket msgPacket) {
                        Map cycleMap = new Gson().fromJson(msgPacket.getDataStr(), Map.class);
                        int cycle = 3600;
                        if (cycleMap.get("cycle") != null) {
                            cycle = Integer.parseInt(cycleMap.get("cycle").toString());
                        }
                        Map<String, Object> map = new Gson().fromJson(response.getDataStr(), Map.class);
                        scheduler.scheduleAtFixedRate(new BackupJob(ioSession, (String) map.get("dbProperties")), 0, cycle, TimeUnit.SECONDS);
                    }
                });

            }
        });

    }

    @Override
    public void stop(IOSession ioSession, MsgPacket msgPacket) {
        scheduler.shutdown();
    }

    @Override
    public void install(IOSession ioSession, MsgPacket msgPacket, HttpRequestInfo httpRequestInfo) {
        new BackupController(ioSession, msgPacket, httpRequestInfo).index();
    }

    @Override
    public void uninstall(IOSession ioSession, MsgPacket msgPacket) {

    }
}
