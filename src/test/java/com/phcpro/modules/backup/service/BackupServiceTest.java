package com.phcpro.modules.backup.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.backup.dto.BackupVerificationDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BackupServiceTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void verifyBackup_comBackupValido_devolveResumo() throws Exception {
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(7L);
        Path backup = writeBackup("company_7_backup_20260617_120000.json", 7L);
        BackupService service = service();

        BackupVerificationDTO result = service.verifyBackup(backup.toString());

        assertEquals("company_7_backup_20260617_120000.json", result.fileName());
        assertEquals(7L, result.companyId());
        assertEquals(14, result.totalSections());
        assertEquals(0, result.itemCounts().get("products"));
    }

    @Test
    void verifyBackup_comEmployee_lancaBusinessRuleException() throws Exception {
        CurrentUserContext.setCurrentUser("caixa", "EMPLOYEE");
        CurrentUserContext.setCurrentCompanyId(7L);
        Path backup = writeBackup("company_7_backup_20260617_120000.json", 7L);

        assertThrows(BusinessRuleException.class, () -> service().verifyBackup(backup.toString()));
    }

    private BackupService service() {
        return new BackupService(null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }

    private Path writeBackup(String fileName, long companyId) throws Exception {
        Path file = tempDir.resolve(fileName);
        Files.writeString(file, """
                {
                  "companyId": %d,
                  "generatedAt": "2026-06-17T12:00:00",
                  "users": [],
                  "companies": [],
                  "clients": [],
                  "suppliers": [],
                  "products": [],
                  "warehouses": [],
                  "stocks": [],
                  "stockMovements": [],
                  "invoices": [],
                  "receipts": [],
                  "purchases": [],
                  "tillSessions": [],
                  "treasuryAccounts": [],
                  "auditLogs": []
                }
                """.formatted(companyId));
        return file;
    }
}
