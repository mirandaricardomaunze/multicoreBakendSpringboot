package com.phcpro.modules.backup.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lógica pura de retenção do backup automático ({@link ScheduledBackupService#isExpiredBackup}).
 */
class ScheduledBackupServiceTest {

    private static final long DAY = 24L * 60 * 60 * 1000;
    private static final long NOW = 1_000_000_000_000L;

    @Test
    void expiraBackupAlemDoPrazo() {
        // 31 dias com retenção de 30 → expirado.
        assertTrue(ScheduledBackupService.isExpiredBackup(
                "multicore_db_20260101_230000.dump", NOW - 31 * DAY, NOW, 30));
    }

    @Test
    void mantemBackupDentroDoPrazo() {
        // 10 dias com retenção de 30 → mantém.
        assertFalse(ScheduledBackupService.isExpiredBackup(
                "multicore_db_20260101_230000.dump", NOW - 10 * DAY, NOW, 30));
    }

    @Test
    void ignoraFicheirosQueNaoSaoDump() {
        assertFalse(ScheduledBackupService.isExpiredBackup(
                "notas.txt", NOW - 100 * DAY, NOW, 30));
        assertFalse(ScheduledBackupService.isExpiredBackup(
                "backup_empresa.json", NOW - 100 * DAY, NOW, 30));
    }

    @Test
    void retencaoZeroOuNegativaRetemTudo() {
        assertFalse(ScheduledBackupService.isExpiredBackup(
                "x.dump", NOW - 999 * DAY, NOW, 0));
        assertFalse(ScheduledBackupService.isExpiredBackup(
                "x.dump", NOW - 999 * DAY, NOW, -1));
    }

    @Test
    void nomeNuloNaoRebenta() {
        assertFalse(ScheduledBackupService.isExpiredBackup(null, 0, NOW, 30));
    }

    @Test
    void extensaoCaseInsensitive() {
        assertTrue(ScheduledBackupService.isExpiredBackup(
                "BACKUP_20260101.DUMP", NOW - 40 * DAY, NOW, 30));
    }
}
