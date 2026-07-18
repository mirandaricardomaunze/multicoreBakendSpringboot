package com.phcpro.modules.backup.service;

import com.phcpro.modules.audit.service.AuditLogService;
import com.phcpro.modules.backup.dto.PhysicalBackupResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Cópia de segurança física <b>automática e agendada</b> da base de dados (recuperação de desastres).
 *
 * <p>Corre num contexto de sistema (sem utilizador autenticado), por isso chama o núcleo do
 * {@link DatabaseBackupService#runPhysicalBackup()} sem verificação de permissão. Depois aplica
 * <b>retenção</b> (apaga backups além do prazo) e <b>regista/alerta</b> o resultado (auditoria +
 * log + estado da última execução para a UI).
 *
 * <p>Só actua quando {@code backup.schedule.enabled=true} (perfis desktop/prod em PostgreSQL). Ver
 * {@code docs/BACKUP_AUTOMATICO_SPEC.md}.
 */
@Service
public class ScheduledBackupService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledBackupService.class);
    private static final long DAY_MS = 24L * 60 * 60 * 1000;

    private final DatabaseBackupService databaseBackupService;
    private final AuditLogService auditLogService;
    private final boolean enabled;
    private final int retentionDays;
    private final String backupDir;

    private volatile LastRun lastRun; // null até à primeira execução

    public ScheduledBackupService(
            DatabaseBackupService databaseBackupService,
            AuditLogService auditLogService,
            @Value("${backup.schedule.enabled:false}") boolean enabled,
            @Value("${backup.retention-days:30}") int retentionDays,
            @Value("${backup.dir:backups}") String backupDir) {
        this.databaseBackupService = databaseBackupService;
        this.auditLogService = auditLogService;
        this.enabled = enabled;
        this.retentionDays = retentionDays;
        this.backupDir = backupDir;
    }

    /** Corre à hora configurada (default 23:00 diário). No-op quando desativado. */
    @Scheduled(cron = "${backup.schedule.cron:0 0 23 * * *}")
    public void scheduledBackup() {
        if (!enabled) return;
        runAndRecord();
    }

    /**
     * Executa o backup físico + retenção e regista o resultado (auditoria + estado). <b>Não lança</b>
     * — uma falha é registada e alertada, não interrompe a app. Devolve o estado da execução.
     */
    public LastRun runAndRecord() {
        try {
            PhysicalBackupResultDTO result = databaseBackupService.runPhysicalBackup();
            int removed = applyRetention();
            String msg = "Backup automático concluído: " + result.filePath()
                    + " (" + (result.sizeBytes() / 1024) + " KB)"
                    + (removed > 0 ? "; " + removed + " backup(s) antigo(s) removido(s)" : "");
            lastRun = new LastRun(LocalDateTime.now(), true, msg);
            auditLogService.logEvent(null, null, "BACKUP_AUTO_OK", msg);
            log.info(msg);
        } catch (RuntimeException ex) {
            String msg = "Backup automático FALHOU: " + ex.getMessage();
            lastRun = new LastRun(LocalDateTime.now(), false, msg);
            auditLogService.logEvent(null, null, "BACKUP_AUTO_FAILED", msg);
            log.error(msg, ex);
        }
        return lastRun;
    }

    /** Apaga backups físicos (.dump) além do prazo de retenção. Devolve quantos removeu. */
    int applyRetention() {
        File dir = new File(backupDir);
        File[] files = dir.listFiles();
        if (files == null) return 0;
        long now = System.currentTimeMillis();
        int removed = 0;
        for (File f : files) {
            if (f.isFile() && isExpiredBackup(f.getName(), f.lastModified(), now, retentionDays)
                    && f.delete()) {
                removed++;
            }
        }
        return removed;
    }

    public LastRun getLastRun() {
        return lastRun;
    }

    public boolean isEnabled() {
        return enabled;
    }

    // ─── Lógica pura (testável sem ambiente) ─────────────────────────────────

    /**
     * É um ficheiro de backup físico ({@code .dump}) elegível para retenção e já além do prazo?
     * Retenção {@code <= 0} significa "reter tudo" (nunca apaga).
     */
    static boolean isExpiredBackup(String name, long lastModifiedMs, long nowMs, int retentionDays) {
        if (retentionDays <= 0) return false;
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".dump")) return false;
        long ageMs = nowMs - lastModifiedMs;
        return ageMs > retentionDays * DAY_MS;
    }

    /** Estado da última execução automática (para visibilidade/alerta na UI). */
    public record LastRun(LocalDateTime time, boolean success, String message) {
    }
}
