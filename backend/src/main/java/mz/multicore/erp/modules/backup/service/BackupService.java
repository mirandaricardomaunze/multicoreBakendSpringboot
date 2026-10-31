package mz.multicore.erp.modules.backup.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.repository.AuditLogRepository;
import mz.multicore.erp.modules.backup.dto.BackupVerificationDTO;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.comercial.repository.ReceiptRepository;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import mz.multicore.erp.modules.financeira.repository.TreasuryTransactionRepository;
import mz.multicore.erp.modules.inventory.repository.StockMovementRepository;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.pos.repository.TillSessionRepository;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import mz.multicore.erp.modules.purchases.repository.SupplierRepository;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
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
            AppUserRepository userRepository,
            CompanyRepository companyRepository,
            ClientRepository clientRepository,
            SupplierRepository supplierRepository,
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository,
            StockRepository stockRepository,
            StockMovementRepository stockMovementRepository,
            InvoiceRepository invoiceRepository,
            ReceiptRepository receiptRepository,
            PurchaseRepository purchaseRepository,
            TillSessionRepository tillSessionRepository,
            TreasuryAccountRepository accountRepository,
            TreasuryTransactionRepository transactionRepository,
            AuditLogRepository auditLogRepository
    ) {
        this(
                new BackupDataCollector(userRepository, companyRepository, clientRepository, supplierRepository,
                        productRepository, warehouseRepository, stockRepository, stockMovementRepository,
                        invoiceRepository, receiptRepository, purchaseRepository, tillSessionRepository,
                        accountRepository, transactionRepository, auditLogRepository),
                new BackupExecutionService(),
                new BackupVerificationService()
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
