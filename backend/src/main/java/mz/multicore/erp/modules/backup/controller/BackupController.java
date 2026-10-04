package mz.multicore.erp.modules.backup.controller;

import jakarta.validation.Valid;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.backup.dto.BackupStatusDTO;
import mz.multicore.erp.modules.backup.dto.BackupVerificationDTO;
import mz.multicore.erp.modules.backup.dto.PhysicalBackupResultDTO;
import mz.multicore.erp.modules.backup.service.BackupService;
import mz.multicore.erp.modules.backup.service.DatabaseBackupService;
import mz.multicore.erp.modules.backup.service.ScheduledBackupService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Backup <b>server-side</b> (cliente-fino): as cópias correm onde está a base de dados — no servidor.
 * Os endpoints executam/listam/verificam ficheiros no directório de backup do servidor e registam a
 * auditoria aqui (o desktop já não chama {@code logEvent}). Só ADMIN.
 */
@RestController
@RequestMapping("/api/backup")
public class BackupController {

    private final BackupService backupService;
    private final DatabaseBackupService databaseBackupService;
    private final ScheduledBackupService scheduledBackupService;
    private final AuditLogService auditLogService;
    private final String backupDir;

    public BackupController(BackupService backupService,
                           DatabaseBackupService databaseBackupService,
                           ScheduledBackupService scheduledBackupService,
                           AuditLogService auditLogService,
                           @Value("${backup.dir:backups}") String backupDir) {
        this.backupService = backupService;
        this.databaseBackupService = databaseBackupService;
        this.scheduledBackupService = scheduledBackupService;
        this.auditLogService = auditLogService;
        this.backupDir = backupDir;
    }

    @GetMapping("/status")
    public BackupStatusDTO status() {
        ScheduledBackupService.LastRun last = scheduledBackupService.getLastRun();
        return new BackupStatusDTO(
                scheduledBackupService.isEnabled(),
                last == null ? null : last.time(),
                last == null ? null : last.success(),
                last == null ? null : last.message());
    }

    @GetMapping("/files")
    public List<String> files() {
        File[] files = new File(backupDir).listFiles();
        if (files == null) return List.of();
        return Arrays.stream(files).filter(File::isFile).map(File::getName).sorted().toList();
    }

    @PostMapping("/auto-run")
    public ScheduledBackupService.LastRun runAuto() {
        requireAdmin();
        ScheduledBackupService.LastRun result = scheduledBackupService.runAndRecord();
        audit("BACKUP_AUTO", "Backup automático executado: " + result.message());
        return result;
    }

    @PostMapping("/logical")
    public LogicalBackupResult runLogical() {
        requireAdmin();
        String path = backupService.executeBackup();
        audit("BACKUP_MANUAL", "Cópia de segurança gerada: " + path);
        return new LogicalBackupResult(path);
    }

    @PostMapping("/physical")
    public PhysicalBackupResultDTO runPhysical() {
        requireAdmin();
        PhysicalBackupResultDTO result = databaseBackupService.executePhysicalBackup();
        audit("BACKUP_FISICO", "Backup físico gerado: " + result.filePath());
        return result;
    }

    @PostMapping("/verify")
    public BackupVerificationDTO verify(@RequestBody @Valid VerifyRequest request) {
        requireAdmin();
        String path = resolveInsideBackupDir(request.fileName()).toString();
        BackupVerificationDTO verification = backupService.verifyBackup(path);
        audit("BACKUP_VERIFY", "Backup verificado: " + verification.fileName());
        return verification;
    }

    /**
     * O nome do ficheiro vem do cliente. Concatená-lo ao directório deixava um ADMIN (ou um token
     * roubado) apontar a {@code ..\..\qualquer-coisa} e fazer o servidor lê-lo. Rejeita-se — em vez
     * de sanear — porque sanear verificaria em silêncio um ficheiro diferente do pedido.
     */
    private Path resolveInsideBackupDir(String fileName) {
        if (fileName == null || fileName.isBlank()
                || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")
                || fileName.contains(":") || fileName.indexOf('\0') >= 0) {
            throw new BusinessRuleException("Nome de ficheiro de cópia de segurança inválido.");
        }
        Path base = Path.of(backupDir).toAbsolutePath().normalize();
        Path target = base.resolve(fileName).normalize();
        if (!target.startsWith(base)) {
            throw new BusinessRuleException("Nome de ficheiro de cópia de segurança inválido.");
        }
        return target;
    }

    private void requireAdmin() {
        if (!"ADMIN".equalsIgnoreCase(CurrentUserContext.getRole())) {
            throw new BusinessRuleException("Apenas administradores podem gerir cópias de segurança.");
        }
    }

    private void audit(String action, String details) {
        auditLogService.logEvent(CurrentUserContext.getUsername(), CurrentUserContext.getCurrentCompanyId(),
                action, details);
    }

    public record LogicalBackupResult(String path) {}

    public record VerifyRequest(
            @jakarta.validation.constraints.NotBlank(message = "O nome do ficheiro é obrigatório.")
            @jakarta.validation.constraints.Size(max = 255, message = "O nome do ficheiro não pode exceder 255 caracteres.")
            String fileName) {}
}
