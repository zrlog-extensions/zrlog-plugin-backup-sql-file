package com.fzb.zrlog.plugin.backup;


import com.fzb.zrlog.plugin.backup.controller.BackupController;
import com.fzb.zrlog.plugin.backup.handler.BackupConnectHandle;
import com.fzb.zrlog.plugin.client.NioClient;
import com.fzb.zrlog.plugin.common.PathKit;
import com.fzb.zrlog.plugin.render.FreeMarkerRenderHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Start {

    public static final String filePath = PathKit.getRootPath() + "/backupSql/";
    public static final int maxBackupSqlFileCount = 20;

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException {
        List<Class> classList = new ArrayList<>();
        classList.add(BackupController.class);
        new NioClient(new BackupConnectHandle(), new FreeMarkerRenderHandler()).connectServerByProperties(args, classList, "/plugin.properties", BackupDbPlugin.class);
    }
}

