package com.zrlog.plugin.backup;

import com.zrlog.plugin.RunConstants;
import com.zrlog.plugin.backup.model.BackupApiResponse;
import com.zrlog.plugin.backup.model.BackupConfig;
import com.zrlog.plugin.backup.model.BackupConfigValues;
import com.zrlog.plugin.backup.model.BackupFileRecord;
import com.zrlog.plugin.backup.model.BackupNotificationChannelInfo;
import com.zrlog.plugin.backup.model.BackupNotificationChannels;
import com.zrlog.plugin.backup.model.BackupNotificationSettingValues;
import com.zrlog.plugin.backup.model.BackupPageData;
import com.zrlog.plugin.backup.model.BackupRequestParams;
import com.zrlog.plugin.backup.model.SiteExportPreviewResponse;
import com.zrlog.plugin.backup.model.SiteImportAiMessageRow;
import com.zrlog.plugin.backup.model.SiteImportArticleRow;
import com.zrlog.plugin.backup.model.SiteImportPrecheckResponse;
import com.zrlog.plugin.backup.model.WebsiteKeyRequest;
import com.zrlog.plugin.backup.controller.BackupController;
import com.zrlog.plugin.backup.scheduler.BackupResultVO;
import com.zrlog.plugin.backup.scheduler.BackupRunResult;
import com.zrlog.plugin.backup.model.RestoreDrillResult;
import com.zrlog.plugin.backup.scheduler.RestoreDrillCapabilityService;
import com.zrlog.plugin.backup.util.NativeUtils;
import com.zrlog.plugin.common.PluginNativeImageUtils;
import com.zrlog.plugin.message.Plugin;
import com.zrlog.plugin.render.SimpleTemplateRender;
import com.zrlog.plugin.type.RunType;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GraalvmAgentApplication {


    public static void main(String[] args) throws IOException {
        RunConstants.runType = RunType.AGENT;
        PluginNativeImageUtils.usedGsonObject();
        PluginNativeImageUtils.gsonNativeAgentByClazz(Arrays.asList(WebsiteKeyRequest.class,
                BackupApiResponse.class, BackupConfig.class, BackupConfigValues.class, BackupFileRecord.class,
                BackupNotificationChannelInfo.class,
                BackupNotificationSettingValues.class, BackupPageData.class, BackupRequestParams.class,
                SiteExportPreviewResponse.class, SiteExportPreviewResponse.SiteExportOptions.class,
                SiteExportPreviewResponse.SiteExportCounts.class, SiteExportPreviewResponse.RedactionEntry.class,
                SiteImportAiMessageRow.class, SiteImportArticleRow.class,
                SiteImportPrecheckResponse.class, SiteImportPrecheckResponse.PrecheckEntry.class,
                BackupResultVO.class,
                BackupRunResult.class,
                RestoreDrillResult.class,
                BackupNotificationChannels.class));
        String fileArch = NativeUtils.getRealFileArch();
        if (Objects.equals(fileArch, "Windows-x86_64")) {
            GraalvmAgentApplication.class.getResourceAsStream("/lib/Windows-x86_64/msvcp120.dll");
        }
        GraalvmAgentApplication.class.getResourceAsStream("/lib/" + fileArch + "/mysqldump");
        String basePath = System.getProperty("user.dir").replace("\\target", "").replace("/target", "");
        File file = new File(basePath + "/src/main/resources");
        PluginNativeImageUtils.doLoopResourceLoad(file.listFiles(), file.getPath() + "/", "/");
        //Application.nativeAgent = true;
        Plugin plugin = new Plugin();
        plugin.setName("test");
        plugin.setDesc("test");
        plugin.setVersion("test");
        Map<String, Object> objectObjectHashMap = new HashMap<>();
        objectObjectHashMap.put("backupCron", "0 2 * * *");
        objectObjectHashMap.put("theme", "light");
        new SimpleTemplateRender().render("/templates/index", plugin, objectObjectHashMap);
        PluginNativeImageUtils.exposeController(Collections.singletonList(BackupController.class));
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
        Application.main(args);

    }
}
