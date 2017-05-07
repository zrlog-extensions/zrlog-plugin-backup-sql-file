package com.fzb.zrlog.plugin.backup.scheduler.handle;

public interface BackupExecution {

    byte[] getDumpFileBytes(String user, String host, String dbName, String password) throws Exception;
}
