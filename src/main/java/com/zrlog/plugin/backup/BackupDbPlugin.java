package com.zrlog.plugin.backup;

import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.api.IPluginAction;
import com.zrlog.plugin.backup.controller.BackupController;
import com.zrlog.plugin.data.codec.HttpRequestInfo;
import com.zrlog.plugin.data.codec.MsgPacket;

public class BackupDbPlugin implements IPluginAction {

    @Override
    public void start(final IOSession ioSession, MsgPacket msgPacket) {
    }

    @Override
    public void stop(IOSession ioSession, MsgPacket msgPacket) {
        //scheduler.shutdown();
    }

    @Override
    public void install(IOSession ioSession, MsgPacket msgPacket, HttpRequestInfo httpRequestInfo) {
        new BackupController(ioSession, msgPacket, httpRequestInfo).index();
    }

    @Override
    public void uninstall(IOSession ioSession, MsgPacket msgPacket) {

    }
}
