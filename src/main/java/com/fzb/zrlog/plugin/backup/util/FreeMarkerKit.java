package com.fzb.zrlog.plugin.backup.util;

import com.fzb.common.util.IOUtil;
import com.fzb.zrlog.plugin.message.Plugin;
import freemarker.template.Configuration;
import freemarker.template.Template;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.Map;

public class FreeMarkerKit {
    private static Configuration cfg = new Configuration(Configuration.VERSION_2_3_0);

    private FreeMarkerKit() {
    }

    public static String render(InputStream in, Map<String, Object> map, Plugin plugin) {
        try {
            Template e = new Template(null, new StringReader(IOUtil.getStringInputStream(in)), cfg);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(out);
            map.put("_plugin", plugin);
            e.process(map, writer);
            writer.flush();
            writer.close();
            return new String(out.toByteArray());
        } catch (Exception var6) {
            var6.printStackTrace();
            return "";
        }
    }
}