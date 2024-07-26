package com.zrlog.plugin.backup.handler;

import com.google.gson.Gson;
import com.zrlog.plugin.IMsgPacketCallBack;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.api.IConnectHandler;
import com.zrlog.plugin.backup.BackupDbPlugin;
import com.zrlog.plugin.backup.scheduler.BackupJob;
import com.zrlog.plugin.common.IdUtil;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.type.ActionType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BackupConnectHandle implements IConnectHandler {

    private ScheduledExecutorService scheduler;

    @Override
    public void handler(IOSession ioSession, MsgPacket msgPacket) {
        refresh(ioSession);
    }

    public void refresh(IOSession ioSession) {
        if (Objects.nonNull(scheduler)) {
            scheduler.shutdown();
        }
        scheduler = Executors.newSingleThreadScheduledExecutor();
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
                scheduler.scheduleAtFixedRate(new BackupJob(ioSession), 0, cycle, TimeUnit.SECONDS);
            }
        });
    }
}
