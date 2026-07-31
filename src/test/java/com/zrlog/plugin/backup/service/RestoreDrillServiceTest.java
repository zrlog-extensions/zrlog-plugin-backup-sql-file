package com.zrlog.plugin.backup.service;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

public class RestoreDrillServiceTest {

    @Test
    public void shouldBuildServerJdbcUrlWithoutSelectingProductionDatabase() {
        assertEquals(
                "jdbc:mysql://127.0.0.1:3306/?useUnicode=true&serverTimezone=UTC",
                RestoreDrillService.serverJdbcUrl(
                        "jdbc:mysql://127.0.0.1:3306/zrlog?useUnicode=true&serverTimezone=UTC"
                )
        );
        assertEquals(
                "jdbc:mysql://db.example.com/",
                RestoreDrillService.serverJdbcUrl("jdbc:mysql://db.example.com/zrlog")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> RestoreDrillService.serverJdbcUrl("jdbc:postgresql://db/zrlog")
        );
    }

    @Test
    public void shouldCalculateSha256ForBackupEvidence() throws Exception {
        File file = File.createTempFile("zrlog-restore-drill", ".sql");
        try {
            Files.write(file.toPath(), "hello".getBytes(StandardCharsets.UTF_8));
            assertEquals(
                    "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                    RestoreDrillService.sha256(file)
            );
        } finally {
            file.delete();
        }
    }

    @Test
    public void shouldPreserveRestoreFailureWhenEvidenceRecordingAlsoFails() {
        RestoreDrillService service = new RestoreDrillService((session, result) -> {
            throw new IllegalStateException("website KV unavailable");
        });

        com.zrlog.plugin.backup.model.RestoreDrillResult result = service.run(null);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Failed to record restore verification evidence"));
        assertTrue(result.getMessage().contains("website KV unavailable"));
        assertTrue(result.getMessage().contains(";"));
    }

    @Test
    public void shouldOnlyTreatTimestampedOldDrillDatabasesAsStale() {
        long now = 2_000_000_000_000L;

        assertTrue(RestoreDrillService.isStaleTemporaryDatabase(
                "zrlog_restore_drill_" + (now - 24L * 60 * 60 * 1000 - 1) + "_abcdef123456", now));
        assertFalse(RestoreDrillService.isStaleTemporaryDatabase(
                "zrlog_restore_drill_" + (now - 60_000) + "_abcdef123456", now));
        assertFalse(RestoreDrillService.isStaleTemporaryDatabase(
                "zrlog_restore_drill_" + (now - 24L * 60 * 60 * 1000 - 1) + "_not-ours", now));
        assertFalse(RestoreDrillService.isStaleTemporaryDatabase("zrlog_restore_drill_manual", now));
        assertFalse(RestoreDrillService.isStaleTemporaryDatabase("production", now));
    }
}
