package com.zrlog.plugin.backup.handler;

import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.api.IConnectHandler;
import com.zrlog.plugin.backup.BackupDbPlugin;
import com.zrlog.plugin.data.codec.MsgPacket;

public class BackupConnectHandle implements IConnectHandler {
    @Override
    public void handler(IOSession ioSession, MsgPacket msgPacket) {
        new BackupDbPlugin().start(ioSession, msgPacket);
    }
}
