package mz.multicore.erp.architecture.security;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.audit.repository.AuditLogRepository;
import mz.multicore.erp.modules.audit.service.ForensicAuditService;
import mz.multicore.erp.modules.backup.service.ScheduledBackupService;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import mz.multicore.erp.modules.financeira.service.CashFlowForecastService;
import mz.multicore.erp.modules.inventory.repository.StockWasteRepository;
import mz.multicore.erp.modules.monitoring.service.SystemMonitoringService;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Harness: Cobertura de Segurança & Bloqueio de Áreas Sensíveis (Fail-Closed)")
class SecurityPermissionGuardCoverageTest {

    private ForensicAuditService forensicAuditService;
    private SystemMonitoringService systemMonitoringService;
    private CashFlowForecastService cashFlowForecastService;

    @BeforeEach
    void setUp() {
        InvoiceRepository invoiceRepository = Mockito.mock(InvoiceRepository.class);
        StockWasteRepository stockWasteRepository = Mockito.mock(StockWasteRepository.class);
        AuditLogRepository auditLogRepository = Mockito.mock(AuditLogRepository.class);
        DataSource dataSource = Mockito.mock(DataSource.class);
        ScheduledBackupService backupService = Mockito.mock(ScheduledBackupService.class);
        TreasuryAccountRepository accountRepository = Mockito.mock(TreasuryAccountRepository.class);
        PurchaseRepository purchaseRepository = Mockito.mock(PurchaseRepository.class);

        forensicAuditService = new ForensicAuditService(invoiceRepository, stockWasteRepository, auditLogRepository);
        systemMonitoringService = new SystemMonitoringService(dataSource, backupService, null, "1.0.0", "backups");
        cashFlowForecastService = new CashFlowForecastService(accountRepository, invoiceRepository, purchaseRepository);

        CurrentUserContext.clear();
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("SEC-01: ForensicAuditService bloqueia acesso sem contexto ou perfil de utilizador comum")
    void testForensicAuditSecurityGuard() {
        // Sem contexto
        assertThrows(BusinessRuleException.class, () -> forensicAuditService.getForensicSummaryForCompany(1L, null, null, null, null, null));

        // Como utilizador comum SELLER
        CurrentUserContext.setCurrentUser("operador_pos", "SELLER");
        CurrentUserContext.setCurrentCompanyId(1L);
        assertThrows(BusinessRuleException.class, () -> forensicAuditService.getForensicSummaryForCompany(1L, null, null, null, null, null));

        // Como MANAGER é permitido
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");
        assertDoesNotThrow(() -> forensicAuditService.getForensicSummaryForCompany(1L, null, null, null, null, null));
    }

    @Test
    @DisplayName("SEC-02: SystemMonitoringService exige perfil ADMIN estrito")
    void testSystemMonitoringSecurityGuard() {
        // Sem contexto
        assertThrows(BusinessRuleException.class, () -> systemMonitoringService.getSystemHealth());

        // Como MANAGER (não é admin)
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");
        CurrentUserContext.setCurrentCompanyId(1L);
        assertThrows(BusinessRuleException.class, () -> systemMonitoringService.getSystemHealth());

        // Como ADMIN é permitido
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        assertDoesNotThrow(() -> systemMonitoringService.getSystemHealth());
    }

    @Test
    @DisplayName("SEC-03: CashFlowForecastService bloqueia acesso a utilizadores não autorizados")
    void testCashFlowForecastSecurityGuard() {
        // Sem contexto
        assertThrows(BusinessRuleException.class, () -> cashFlowForecastService.generateForecastForCompany(1L, null));

        // Como EMPLOYEE
        CurrentUserContext.setCurrentUser("funcionario", "EMPLOYEE");
        CurrentUserContext.setCurrentCompanyId(1L);
        assertThrows(BusinessRuleException.class, () -> cashFlowForecastService.generateForecastForCompany(1L, null));

        // Como MANAGER é permitido
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");
        assertDoesNotThrow(() -> cashFlowForecastService.generateForecastForCompany(1L, null));
    }
}
