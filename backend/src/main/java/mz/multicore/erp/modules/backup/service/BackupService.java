package mz.multicore.erp.modules.backup.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.repository.AuditLogRepository;
import mz.multicore.erp.modules.backup.dto.BackupVerificationDTO;
import mz.multicore.erp.modules.comercial.service.ClientLookup;
import mz.multicore.erp.modules.comercial.service.InvoiceLookup;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.comercial.service.ReceiptLookup;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.financeira.service.TreasuryAccountLookup;
import mz.multicore.erp.modules.inventory.service.StockLookup;
import mz.multicore.erp.modules.inventory.service.StockMovementLookup;
import mz.multicore.erp.modules.inventory.service.WarehouseLookup;
import mz.multicore.erp.modules.pos.service.TillSessionLookup;
import mz.multicore.erp.modules.purchases.service.SupplierLookup;
import mz.multicore.erp.modules.users.service.AppUserLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Orquestrador transacional de cópias de segurança lógicas (backup JSON).
 * Delega a extração a {@link BackupDataCollector}, a persistência a {@link BackupExecutionService}
 * e a integridade a {@link BackupVerificationService}.
 */
@Service
public class BackupService {

    private final BackupDataCollector dataCollector;
    private final BackupExecutionService executionService;
    private final BackupVerificationService verificationService;

    @Autowired
    public BackupService(
            BackupDataCollector dataCollector,
            BackupExecutionService executionService,
            BackupVerificationService verificationService
    ) {
        this.dataCollector = dataCollector;
        this.executionService = executionService;
        this.verificationService = verificationService;
    }

    public BackupService(
            AppUserLookup userLookup,
            CompanyLookup companyLookup,
            ClientLookup clientLookup,
            SupplierLookup supplierLookup,
            ProductLookup productLookup,
            WarehouseLookup warehouseLookup,
            StockLookup stockLookup,
            StockMovementLookup stockMovementLookup,
            InvoiceLookup invoiceLookup,
            ReceiptLookup receiptLookup,
            TillSessionLookup tillSessionLookup,
            TreasuryAccountLookup treasuryAccountLookup,
            AuditLogRepository auditLogRepository
    ) {
        this(
                new BackupDataCollector(userLookup, companyLookup, clientLookup, supplierLookup,
                        productLookup, warehouseLookup, stockLookup, stockMovementLookup,
                        invoiceLookup, receiptLookup, tillSessionLookup,
                        treasuryAccountLookup, auditLogRepository),
                new BackupExecutionService(),
                new BackupVerificationService(new BackupContentReader(new BackupFileCipher("")))
        );
    }

    @Transactional(readOnly = true)
    public synchronized String executeBackup() {
        PermissionGuard.requireAdmin("gerar cópia de segurança");
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        Map<String, Object> data = dataCollector.collectBackupData(companyId);
        return executionService.writeBackupFile(companyId, data);
    }

    public BackupVerificationDTO verifyBackup(String path) {
        return verificationService.verifyBackup(path);
    }
}
