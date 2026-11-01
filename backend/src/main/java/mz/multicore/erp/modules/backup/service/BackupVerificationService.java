package mz.multicore.erp.modules.backup.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.backup.dto.BackupVerificationDTO;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

/**
 * Validador de integridade e conteúdo de ficheiros de cópia de segurança (backup).
 */
@Service
public class BackupVerificationService {

    public static final List<String> REQUIRED_COLLECTIONS = List.of(
            "users",
            "companies",
            "clients",
            "suppliers",
            "products",
            "warehouses",
            "stocks",
            "stockMovements",
            "invoices",
            "receipts",
            "purchases",
            "tillSessions",
            "treasuryAccounts",
            "auditLogs"
    );

    private final BackupContentReader reader;

    public BackupVerificationService(BackupContentReader reader) {
        this.reader = reader;
    }

    public BackupVerificationDTO verifyBackup(String path) {
        PermissionGuard.requireAdmin("verificar cópia de segurança");
        if (path == null || path.isBlank()) {
            throw new BusinessRuleException("Selecione um ficheiro de backup para verificar.");
        }

        File backupFile = new File(path);
        if (!backupFile.exists() || !backupFile.isFile()) {
            throw new BusinessRuleException("Ficheiro de backup não encontrado.");
        }
        Map<String, Object> data = reader.read(backupFile);
        Long currentCompanyId = CurrentUserContext.getCurrentCompanyId();
        Long backupCompanyId = readCompanyId(data.get("companyId"));
        if (!currentCompanyId.equals(backupCompanyId)) {
            throw new BusinessRuleException("O backup pertence a outra empresa.");
        }
        Object generatedAt = data.get("generatedAt");
        if (!(generatedAt instanceof String generatedAtText) || generatedAtText.isBlank()) {
            throw new BusinessRuleException("Backup inválido: data de geração ausente.");
        }

        Map<String, Integer> itemCounts = new LinkedHashMap<>();
        for (String section : REQUIRED_COLLECTIONS) {
            Object value = data.get(section);
            if (!(value instanceof List<?> items)) {
                throw new BusinessRuleException("Backup inválido: secção '" + section + "' ausente ou corrompida.");
            }
            itemCounts.put(section, items.size());
        }

        return new BackupVerificationDTO(
                backupFile.getName(),
                backupCompanyId,
                generatedAtText,
                REQUIRED_COLLECTIONS.size(),
                itemCounts
        );
    }

    private Long readCompanyId(Object rawCompanyId) {
        if (rawCompanyId instanceof Number number) {
            return number.longValue();
        }
        if (rawCompanyId instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                throw new BusinessRuleException("Backup inválido: empresa com identificador inválido.");
            }
        }
        throw new BusinessRuleException("Backup inválido: empresa ausente.");
    }
}
