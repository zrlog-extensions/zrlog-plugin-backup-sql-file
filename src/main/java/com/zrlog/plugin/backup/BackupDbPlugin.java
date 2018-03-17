package com.zrlog.plugin.backup;

import com.zrlog.plugin.IMsgPacketCallBack;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.RunConstants;
import com.zrlog.plugin.api.IPluginAction;
import com.zrlog.plugin.backup.controller.BackupController;
import com.zrlog.plugin.backup.scheduler.BackupJob;
import com.zrlog.plugin.common.IdUtil;
import com.zrlog.plugin.data.codec.HttpRequestInfo;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.type.ActionType;
import com.zrlog.plugin.type.RunType;
import com.google.gson.Gson;
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
                        Map cycleMap = new Gson().fromJson(msgPacket.getDataStr(), Map.class);
                        int cycle = 1;
                        if (cycleMap.get("cycle") != null) {
                            cycle = Integer.parseInt(cycleMap.get("cycle").toString()) / 3600;
                        }
                        try {
                            BackupJob.clearFile();
                            scheduler = schedulerFactory.getScheduler();
                            JobDetail backupJob = JobBuilder.newJob(BackupJob.class)
                                    .withIdentity("sql", "backup").build();

                            Map<String, Object> map = new Gson().fromJson(response.getDataStr(), Map.class);
                            backupJob.getJobDataMap().put("dbProperties", map.get("dbProperties"));
                            backupJob.getJobDataMap().put("cycle", cycle);
                            String cron;
                            if (cycle < 24) {
                                cron = "0 0 */" + cycle + " * * ?";
                            } else {
                                cron = "0 0 0 */" + cycle / 24 + " * ?";
                            }
                            //开发环境（每分钟执行一次）
                            if (RunConstants.runType == RunType.DEV) {
                                cron = "0 */1 * * * ?";
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
