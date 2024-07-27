package com.zrlog.plugin.backup.scheduler;

import java.io.File;

public record BackupFileInfo(File resultFile, String sourceFileMd5) {
}
