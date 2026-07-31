package com.zrlog.plugin.backup;


import com.zrlog.plugin.backup.controller.BackupController;
import com.zrlog.plugin.backup.scheduler.BackupCapabilityService;
import com.zrlog.plugin.backup.scheduler.RestoreDrillCapabilityService;
import com.zrlog.plugin.client.NioClient;
import com.zrlog.plugin.common.PathKit;
import com.zrlog.plugin.render.SimpleTemplateRender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Application {

    public static final String sqlPath = PathKit.getRootPath() + "/sql/";
    public static final int maxBackupSqlFileCount = 20;

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException {
        List<Class<?>> classList = new ArrayList<>();
        classList.add(BackupController.class);
        List<Class<? extends com.zrlog.plugin.api.IPluginService>> services = new ArrayList<>();
        services.add(BackupCapabilityService.class);
        services.add(RestoreDrillCapabilityService.class);
        new NioClient(null, new SimpleTemplateRender())
                .connectServer(args, classList, BackupDbPlugin.class, services);
    }
}
