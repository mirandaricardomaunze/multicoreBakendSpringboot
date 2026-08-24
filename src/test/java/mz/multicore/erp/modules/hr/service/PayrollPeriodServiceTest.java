package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.hr.model.PayrollPeriod;
import mz.multicore.erp.modules.hr.repository.PayrollPeriodRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** B8.6 do harness do RH (RHC-74): o mês da folha fecha. */
class PayrollPeriodServiceTest {

    private static final Long COMPANY = 7L;

    private PayrollPeriodRepository periodRepository;
    private CompanyRepository companyRepository;
    private AuditLogService auditLogService;
    private PayrollPeriodService service;

    @BeforeEach
    void setUp() {
        periodRepository = mock(PayrollPeriodRepository.class);
        companyRepository = mock(CompanyRepository.class);
        auditLogService = mock(AuditLogService.class);
        service = new PayrollPeriodService(periodRepository, companyRepository, auditLogService);

        Company company = new Company();
        company.setId(COMPANY);
        when(companyRepository.findById(COMPANY)).thenReturn(Optional.of(company));
        when(periodRepository.save(any(PayrollPeriod.class))).thenAnswer(inv -> inv.getArgument(0));
        CurrentUserContext.setCurrentCompanyId(COMPANY);
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void aPeriodWithNoRowIsOpen() {
        // Não se exige um passo de abertura que ninguém sabe que existe — mesma decisão do TimeSheet.
        when(periodRepository.findByCompanyIdAndYearAndMonth(COMPANY, 2026, 8))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.ensureOpen(2026, 8));
    }

    @Test
    void closingCreatesTheRowAndNamesWhoClosedIt() {
        when(periodRepository.findByCompanyIdAndYearAndMonth(COMPANY, 2026, 8))
                .thenReturn(Optional.empty());

        var dto = service.close(2026, 8);

        assertEquals("FECHADO", dto.status());
        assertEquals("gestor", dto.closedBy());
        verify(auditLogService).logCurrent(eq("PAYROLL_PERIOD_CLOSE"), any());
    }

    @Test
    void aClosedPeriod_refusesNewPayslipsByName() { // RHC-74
        when(periodRepository.findByCompanyIdAndYearAndMonth(COMPANY, 2026, 8))
                .thenReturn(Optional.of(closed()));

        var ex = assertThrows(BusinessRuleException.class, () -> service.ensureOpen(2026, 8));

        assertTrue(ex.getMessage().contains("8/2026"));
        assertTrue(ex.getMessage().contains("retenção declarada"));
    }

    @Test
    void reopeningWithoutAReason_isRefused() {
        // Um fecho que se desfaz em silêncio não protege nada.
        var ex = assertThrows(BusinessRuleException.class, () -> service.reopen(2026, 8, "  "));

        assertTrue(ex.getMessage().contains("motivo"));
        verify(periodRepository, never()).save(any());
    }

    @Test
    void reopeningIsAuditedWithTheReason() {
        when(periodRepository.findByCompanyIdAndYearAndMonth(COMPANY, 2026, 8))
                .thenReturn(Optional.of(closed()));

        var dto = service.reopen(2026, 8, "Recibo em falta da Ana Sitoe");

        assertEquals("ABERTO", dto.status());
        assertEquals("Recibo em falta da Ana Sitoe", dto.reopenReason());
        verify(auditLogService).logCurrent(eq("PAYROLL_PERIOD_REOPEN"), any());
    }

    @Test
    void employeeRole_cannotClose() {
        CurrentUserContext.setCurrentUser("operador", "EMPLOYEE");

        assertThrows(BusinessRuleException.class, () -> service.close(2026, 8));
        verify(periodRepository, never()).save(any());
    }

    private PayrollPeriod closed() {
        Company company = new Company();
        company.setId(COMPANY);
        PayrollPeriod period = new PayrollPeriod();
        period.setId(1L);
        period.setCompany(company);
        period.setYear(2026);
        period.setMonth(8);
        period.setStatus("FECHADO");
        period.setClosedBy("gestor");
        return period;
    }
}
