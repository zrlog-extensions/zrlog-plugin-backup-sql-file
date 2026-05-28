package com.zrlog.plugin.backup;

import com.zrlog.plugin.backup.controller.BackupController;
import com.zrlog.plugin.backup.util.NativeUtils;
import com.zrlog.plugin.common.PluginNativeImageUtils;
import com.zrlog.plugin.message.Plugin;
import com.zrlog.plugin.render.SimpleTemplateRender;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GraalvmAgentApplication {


    public static void main(String[] args) throws IOException {
        PluginNativeImageUtils.usedGsonObject();
        String fileArch = NativeUtils.getRealFileArch();
        if (Objects.equals(fileArch, "Windows-x86_64")) {
            GraalvmAgentApplication.class.getResourceAsStream("/lib/Windows-x86_64/msvcp120.dll");
        }
        GraalvmAgentApplication.class.getResourceAsStream("/lib/" + fileArch + "/mysqldump");
        String basePath = System.getProperty("user.dir").replace("\\target", "").replace("/target", "");
        File file = new File(basePath + "/src/main/resources");
        PluginNativeImageUtils.doLoopResourceLoad(file.listFiles(), file.getPath() + "/templates", "/templates");
        //Application.nativeAgent = true;
        Plugin plugin = new Plugin();
        plugin.setName("test");
        plugin.setDesc("test");
        plugin.setVersion("test");
        Map<String, Object> objectObjectHashMap = new HashMap<>();
        objectObjectHashMap.put("cycle", "3600");
        objectObjectHashMap.put("theme", "light");
        new SimpleTemplateRender().render("/templates/index", plugin, objectObjectHashMap);
        PluginNativeImageUtils.exposeController(Collections.singletonList(BackupController.class));
        Application.main(args);

    }
}