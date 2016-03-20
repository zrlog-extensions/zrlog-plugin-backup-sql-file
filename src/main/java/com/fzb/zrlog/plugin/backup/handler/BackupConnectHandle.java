package com.fzb.zrlog.plugin.backup.handler;

import com.fzb.zrlog.plugin.IOSession;
import com.fzb.zrlog.plugin.api.IConnectHandler;
import com.fzb.zrlog.plugin.backup.BackupDbPlugin;
import com.fzb.zrlog.plugin.data.codec.MsgPacket;

public class BackupConnectHandle implements IConnectHandler {
    @Override
    public void handler(IOSession ioSession, MsgPacket msgPacket) {
        new BackupDbPlugin().start(ioSession, msgPacket);
    }
}
