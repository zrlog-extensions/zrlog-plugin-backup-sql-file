package com.fzb.zrlog.plugin.backup;

import com.fzb.common.util.RunConstants;
import com.fzb.zrlog.plugin.IMsgPacketCallBack;
import com.fzb.zrlog.plugin.IOSession;
import com.fzb.zrlog.plugin.api.IPluginAction;
import com.fzb.zrlog.plugin.backup.controller.BackupController;
import com.fzb.zrlog.plugin.backup.scheduler.BackUpJob;
import com.fzb.zrlog.plugin.common.IdUtil;
import com.fzb.zrlog.plugin.data.codec.HttpRequestInfo;
import com.fzb.zrlog.plugin.data.codec.MsgPacket;
import com.fzb.zrlog.plugin.data.codec.MsgPacketStatus;
import com.fzb.zrlog.plugin.type.ActionType;
import com.fzb.zrlog.plugin.type.RunType;
import flexjson.JSONDeserializer;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.HashMap;
import java.util.Map;

public class BackupDbPlugin implements IPluginAction {

    private Scheduler scheduler;
    private SchedulerFactory schedulerFactory;

    @Override
    public void start(final IOSession ioSession, MsgPacket msgPacket) {
        schedulerFactory = new StdSchedulerFactory();

        ioSession.sendJsonMsg(new HashMap<>(), ActionType.GET_DB_PROPERTIES.name(), IdUtil.getInt(), MsgPacketStatus.SEND_REQUEST, new IMsgPacketCallBack() {
            @Override
            public void handler(final MsgPacket response) {
                Map<String, Object> keyMap = new HashMap<>();
                keyMap.put("key", "cycle");
                ioSession.sendJsonMsg(keyMap, ActionType.GET_WEBSITE.name(), IdUtil.getInt(), MsgPacketStatus.SEND_REQUEST, new IMsgPacketCallBack() {
                    @Override
                    public void handler(MsgPacket msgPacket) {
                        Map cycleMap = new JSONDeserializer<Map>().deserialize(msgPacket.getDataStr());
                        int cycle = 1;
                        if (cycleMap.get("cycle") != null) {
                            cycle = Integer.parseInt(cycleMap.get("cycle").toString()) / 3600;
                        }
                        try {
                            scheduler = schedulerFactory.getScheduler();
                            JobDetail backupJob = JobBuilder.newJob(BackUpJob.class)
                                    .withIdentity("sql", "backup").build();
                            Map<String, Object> map = new JSONDeserializer<Map<String, Object>>().deserialize(response.getDataStr());
                            backupJob.getJobDataMap().put("dbProperties", map.get("dbProperties"));
                            backupJob.getJobDataMap().put("cycle", cycle);

                            String cron = "0 0 */" + cycle + " * * ?";
                            //开发环境
                            if (RunConstants.runType == RunType.DEV) {
                                cron = "0 */" + cycle + " * * * ?";
                            }
                            CronTrigger trigger = TriggerBuilder
                                    .newTrigger()
                                    .withIdentity("sql", "backup")
                                    .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                                    .build();

                            scheduler.scheduleJob(backupJob, trigger);
                            scheduler.start();
                        } catch (SchedulerException e) {
                            e.printStackTrace();
                        }
                    }
                });

            }
        });

    }

    @Override
    public void stop(IOSession ioSession, MsgPacket msgPacket) {
        try {
            schedulerFactory.getScheduler().clear();
            scheduler.clear();
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void install(IOSession ioSession, MsgPacket msgPacket, HttpRequestInfo httpRequestInfo) {
        new BackupController(ioSession, msgPacket, httpRequestInfo).index();
    }

    @Override
    public void uninstall(IOSession ioSession, MsgPacket msgPacket) {

    }
}
