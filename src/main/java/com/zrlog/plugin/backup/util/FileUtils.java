package com.zrlog.plugin.backup.util;

import java.io.File;
import java.nio.file.Path;

public class FileUtils {

    public static File safeAppendFilePath(String basePath, String appendFilePath) {
        Path resolvedPath = new File(basePath).toPath().resolve(new File(basePath + "/" + appendFilePath).toPath()).normalize();
        if (!resolvedPath.startsWith(basePath)) {
            throw new IllegalArgumentException("Invalid file path " + appendFilePath);
        }
        return resolvedPath.toFile();
    }
}
