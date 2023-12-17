package com.zrlog.plugin.backup.scheduler;

import java.io.File;

public record BackupResultVO(File file, boolean newFile) {
}
