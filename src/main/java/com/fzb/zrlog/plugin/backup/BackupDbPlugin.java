package com.fzb.zrlog.plugin.backup;

import com.fzb.zrlog.plugin.IMsgPacketCallBack;
import com.fzb.zrlog.plugin.IOSession;
import com.fzb.zrlog.plugin.api.IPluginAction;
import com.fzb.zrlog.plugin.backup.scheduler.BackUpJob;
import com.fzb.zrlog.plugin.common.IdUtil;
import com.fzb.zrlog.plugin.data.codec.HttpRequestInfo;
import com.fzb.zrlog.plugin.data.codec.MsgPacket;
import com.fzb.zrlog.plugin.backup.controller.BackupController;
import com.fzb.zrlog.plugin.data.codec.MsgPacketStatus;
import com.fzb.zrlog.plugin.type.ActionType;
import flexjson.JSONDeserializer;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.HashMap;
import java.util.Map;

public class BackupDbPlugin implements IPluginAction {

    private Scheduler scheduler;
    private SchedulerFactory schedulerFactory;

    @Override
    public void start(IOSession ioSession, MsgPacket msgPacket) {
        schedulerFactory = new StdSchedulerFactory();

        ioSession.sendJsonMsg(new HashMap<>(), ActionType.GET_DB_PROPERTIES.name(), IdUtil.getInt(), MsgPacketStatus.SEND_REQUEST, new IMsgPacketCallBack() {
            @Override
            public void handler(MsgPacket response) {
                try {
                    scheduler = schedulerFactory.getScheduler();
                    JobDetail backupJob = JobBuilder.newJob(BackUpJob.class)
                            .withIdentity("sql", "backup").build();
                    Map<String, Object> map = new JSONDeserializer<Map<String, Object>>().deserialize(response.getDataStr());
                    backupJob.getJobDataMap().put("dbProperties", map.get("dbProperties"));

                    CronTrigger trigger = TriggerBuilder
                            .newTrigger()
                            .withIdentity("sql", "backup")
                            .withSchedule(CronScheduleBuilder.cronSchedule("0 0 * * * ?"))
                            .build();

                    scheduler.scheduleJob(backupJob, trigger);
                    scheduler.start();
                } catch (SchedulerException e) {
                    e.printStackTrace();
                }
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
