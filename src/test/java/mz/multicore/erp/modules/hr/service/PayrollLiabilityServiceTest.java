package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.events.PayrollLiabilityDeliveredEvent;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.hr.dto.PayrollLiabilityDTO;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.PayrollLiability;
import mz.multicore.erp.modules.hr.model.PayrollLiabilityStatus;
import mz.multicore.erp.modules.hr.model.PayrollLiabilityType;
import mz.multicore.erp.modules.hr.model.Payslip;
import mz.multicore.erp.modules.hr.repository.PayrollLiabilityRepository;
import mz.multicore.erp.modules.hr.repository.PayslipRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * B5 do harness do RH (RHC-50..52, RHC-55).
 *
 * <p>O caso que carrega a classe é o primeiro: até aqui, pagar a folha fazia sair só o líquido e o
 * dinheiro do Estado ficava na conta da empresa <b>sem estar marcado como dívida</b>.
 */
class PayrollLiabilityServiceTest {

    private static final Long COMPANY = 7L;

    private PayrollLiabilityRepository liabilityRepository;
    private PayslipRepository payslipRepository;
    private CompanyRepository companyRepository;
    private HrPolicyService hrPolicyService;
    private FinanceService financeService;
    private AuditLogService auditLogService;
    private ApplicationEventPublisher eventPublisher;
    private PayrollLiabilityService service;

    @BeforeEach
    void setUp() {
        liabilityRepository = mock(PayrollLiabilityRepository.class);
        payslipRepository = mock(PayslipRepository.class);
        companyRepository = mock(CompanyRepository.class);
        hrPolicyService = mock(HrPolicyService.class);
        financeService = mock(FinanceService.class);
        auditLogService = mock(AuditLogService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new PayrollLiabilityService(liabilityRepository, payslipRepository,
                companyRepository, hrPolicyService, financeService, auditLogService, eventPublisher);

        Company company = new Company();
        company.setId(COMPANY);
        when(companyRepository.findById(COMPANY)).thenReturn(Optional.of(company));
        when(liabilityRepository.save(any(PayrollLiability.class))).thenAnswer(inv -> inv.getArgument(0));
        when(hrPolicyService.deliveryDeadline(any(), anyInt(), anyInt())).thenReturn(Optional.empty());
        CurrentUserContext.setCurrentCompanyId(COMPANY);
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void payingPayroll_raisesTheThreeLiabilities() { // RHC-50 — o 🔴 do bloco
        // Até aqui só o líquido saía. O IRPS e as duas quotas do INSS eram impressos no mapa fiscal
        // e desapareciam: nenhuma linha de código voltava a tocar-lhes.
        when(payslipRepository.findByCompanyIdAndYearAndMonth(COMPANY, 2026, 8))
                .thenReturn(List.of(paidPayslip("1200", "900", "1500"),
                                    paidPayslip("800", "600", "1000")));

        List<PayrollLiabilityDTO> raised = service.accrueForPeriod(2026, 8);

        assertEquals(3, raised.size());
        assertEquals(0, new BigDecimal("2000").compareTo(amountOf(raised, "IRPS")));
        assertEquals(0, new BigDecimal("1500").compareTo(amountOf(raised, "INSS_TRABALHADOR")));
        assertEquals(0, new BigDecimal("2500").compareTo(amountOf(raised, "INSS_PATRONAL")));
        assertTrue(raised.stream().allMatch(l -> "POR_ENTREGAR".equals(l.status())));
    }

    @Test
    void onlyPaidPayslipsCount() {
        // A retenção acontece no pagamento. Um recibo em rascunho ainda não reteve nada a ninguém.
        Payslip draft = paidPayslip("5000", "5000", "5000");
        draft.setStatus("DRAFT");
        when(payslipRepository.findByCompanyIdAndYearAndMonth(COMPANY, 2026, 8))
                .thenReturn(List.of(paidPayslip("1200", "900", "1500"), draft));

        List<PayrollLiabilityDTO> raised = service.accrueForPeriod(2026, 8);

        assertEquals(0, new BigDecimal("1200").compareTo(amountOf(raised, "IRPS")));
    }

    @Test
    void accrueIsIdempotent_theSecondRunUpdatesInsteadOfDuplicating() {
        // Pagar o mesmo recibo duas vezes não pode duplicar a dívida ao Estado. Reapurar (em vez de
        // somar recibo a recibo) é o que torna isto verdade sem estado extra.
        PayrollLiability existing = pending(PayrollLiabilityType.IRPS, "1200");
        when(liabilityRepository.findByCompanyIdAndYearAndMonthAndLiabilityType(
                COMPANY, 2026, 8, PayrollLiabilityType.IRPS)).thenReturn(Optional.of(existing));
        when(payslipRepository.findByCompanyIdAndYearAndMonth(COMPANY, 2026, 8))
                .thenReturn(List.of(paidPayslip("1200", "900", "1500"),
                                    paidPayslip("800", "600", "1000")));

        List<PayrollLiabilityDTO> raised = service.accrueForPeriod(2026, 8);

        assertEquals(0, new BigDecimal("2000").compareTo(amountOf(raised, "IRPS")));
        assertEquals(0, new BigDecimal("2000").compareTo(existing.getAmount()));
    }

    @Test
    void withoutAConfiguredDeadline_theObligationIsStillBorn() { // RHC-52
        // Não saber o prazo nunca foi razão para perder o rasto do dinheiro. Nasce sem data, e o
        // painel diz "prazo por configurar" em vez de inventar um.
        when(payslipRepository.findByCompanyIdAndYearAndMonth(COMPANY, 2026, 8))
                .thenReturn(List.of(paidPayslip("1200", "900", "1500")));

        List<PayrollLiabilityDTO> raised = service.accrueForPeriod(2026, 8);

        assertNull(raised.get(0).dueDate());
        assertFalse(raised.get(0).overdue());
    }

    @Test
    void withAConfiguredDeadline_theDueDateIsDated() {
        when(hrPolicyService.deliveryDeadline(PayrollLiabilityType.IRPS, 2026, 8))
                .thenReturn(Optional.of(LocalDate.of(2026, 9, 20)));
        when(payslipRepository.findByCompanyIdAndYearAndMonth(COMPANY, 2026, 8))
                .thenReturn(List.of(paidPayslip("1200", "900", "1500")));

        List<PayrollLiabilityDTO> raised = service.accrueForPeriod(2026, 8);

        assertEquals(LocalDate.of(2026, 9, 20),
                raised.stream().filter(l -> "IRPS".equals(l.liabilityType())).findFirst()
                        .orElseThrow().dueDate());
    }

    @Test
    void markDelivered_leavesTreasuryAuditAndPosting() { // RHC-51
        when(liabilityRepository.findByIdAndCompanyId(9L, COMPANY))
                .thenReturn(Optional.of(pending(PayrollLiabilityType.IRPS, "2000")));

        PayrollLiabilityDTO delivered = service.markDelivered(9L, "M-Pesa 8891");

        assertEquals("ENTREGUE", delivered.status());
        assertEquals("gestor", delivered.deliveredBy());
        assertEquals(LocalDate.now(), delivered.paymentDate());
        verify(financeService).registerAutoPayout(eq(new BigDecimal("2000")), any());
        verify(auditLogService).logCurrent(eq("PAYROLL_LIABILITY_DELIVERED"), any());
        verify(eventPublisher).publishEvent(any(PayrollLiabilityDeliveredEvent.class));
    }

    @Test
    void markDelivered_twice_isRefused() { // RHC-51
        // É a forma mais fácil de a empresa pagar ao Estado a dobrar e só dar por isso na
        // reconciliação — meses depois.
        PayrollLiability already = pending(PayrollLiabilityType.IRPS, "2000");
        already.setStatus(PayrollLiabilityStatus.ENTREGUE);
        already.setPaymentDate(LocalDate.of(2026, 9, 18));
        when(liabilityRepository.findByIdAndCompanyId(9L, COMPANY)).thenReturn(Optional.of(already));

        var ex = assertThrows(BusinessRuleException.class, () -> service.markDelivered(9L, "outra ref"));

        assertTrue(ex.getMessage().contains("já foi entregue"));
        verify(financeService, never()).registerAutoPayout(any(), any());
    }

    @Test
    void employeeRole_cannotDeliver() {
        CurrentUserContext.setCurrentUser("operador", "EMPLOYEE");

        assertThrows(BusinessRuleException.class, () -> service.markDelivered(9L, null));
        verify(financeService, never()).registerAutoPayout(any(), any());
    }

    @Test
    void aNewPayslipInAnAlreadyDeliveredPeriod_isRefusedByName() {
        // Alterar em silêncio um valor já declarado ao Estado é pior do que não deixar pagar.
        PayrollLiability delivered = pending(PayrollLiabilityType.IRPS, "1200");
        delivered.setStatus(PayrollLiabilityStatus.ENTREGUE);
        when(liabilityRepository.findByCompanyIdAndYearAndMonthAndLiabilityType(
                COMPANY, 2026, 8, PayrollLiabilityType.IRPS)).thenReturn(Optional.of(delivered));
        when(payslipRepository.findByCompanyIdAndYearAndMonth(COMPANY, 2026, 8))
                .thenReturn(List.of(paidPayslip("1200", "900", "1500"),
                                    paidPayslip("800", "600", "1000")));

        var ex = assertThrows(BusinessRuleException.class, () -> service.accrueForPeriod(2026, 8));

        assertTrue(ex.getMessage().contains("já foi entregue ao Estado"));
        assertTrue(ex.getMessage().contains("8/2026"));
    }

    @Test
    void dueAlerts_includeTheUndatedOnes() { // RHC-52
        // Uma obrigação sem prazo nunca chega a estar atrasada — e por isso nunca apareceria em
        // lista nenhuma. É precisamente a que não pode ficar escondida.
        PayrollLiability undated = pending(PayrollLiabilityType.IRPS, "2000");
        PayrollLiability faraway = pending(PayrollLiabilityType.INSS_TRABALHADOR, "1500");
        faraway.setDueDate(LocalDate.now().plusMonths(2));
        PayrollLiability overdue = pending(PayrollLiabilityType.INSS_PATRONAL, "2500");
        overdue.setDueDate(LocalDate.now().minusDays(3));
        when(liabilityRepository.findByStatus(COMPANY, PayrollLiabilityStatus.POR_ENTREGAR))
                .thenReturn(List.of(undated, faraway, overdue));

        List<PayrollLiabilityDTO> alerts = service.dueAlerts();

        assertEquals(2, alerts.size());
        assertTrue(alerts.stream().anyMatch(l -> "IRPS".equals(l.liabilityType()) && l.dueDate() == null));
        assertTrue(alerts.stream().anyMatch(PayrollLiabilityDTO::overdue));
    }

    @Test
    void monthlyCost_countsTheEmployerInssOnTop() { // RHC-55
        // O ilíquido não é o que a empresa gasta. O patronal era impresso no mapa fiscal e não
        // aparecia em relatório nenhum de custo.
        when(payslipRepository.findByCompanyIdAndYearAndMonth(COMPANY, 2026, 8))
                .thenReturn(List.of(paidPayslip("1200", "900", "1500")));

        var cost = service.monthlyCost(2026, 8);

        assertEquals(0, new BigDecimal("30000").compareTo(cost.grossPay()));
        assertEquals(0, new BigDecimal("1500").compareTo(cost.employerInss()));
        assertEquals(0, new BigDecimal("31500").compareTo(cost.totalCost()));
    }

    // ─── Apoio ────────────────────────────────────────────────────────────────

    private static BigDecimal amountOf(List<PayrollLiabilityDTO> liabilities, String type) {
        return liabilities.stream().filter(l -> type.equals(l.liabilityType()))
                .findFirst().orElseThrow().amount();
    }

    private PayrollLiability pending(PayrollLiabilityType type, String amount) {
        Company company = new Company();
        company.setId(COMPANY);
        PayrollLiability liability = new PayrollLiability();
        liability.setId(9L);
        liability.setCompany(company);
        liability.setYear(2026);
        liability.setMonth(8);
        liability.setLiabilityType(type);
        liability.setAmount(new BigDecimal(amount));
        liability.setStatus(PayrollLiabilityStatus.POR_ENTREGAR);
        return liability;
    }

    private Payslip paidPayslip(String irps, String employeeInss, String employerInss) {
        Employee employee = new Employee();
        employee.setId(5L);
        employee.setEmployeeNumber("EMP-5");
        employee.setName("Maria");

        Payslip p = new Payslip();
        p.setEmployee(employee);
        p.setYear(2026);
        p.setMonth(8);
        p.setStatus("PAID");
        p.setBaseSalary(new BigDecimal("30000"));
        p.setAllowances(BigDecimal.ZERO);
        p.setOvertime(BigDecimal.ZERO);
        p.setIrpsDeduction(new BigDecimal(irps));
        p.setInssDeduction(new BigDecimal(employeeInss));
        p.setEmployerInss(new BigDecimal(employerInss));
        p.setNetPay(new BigDecimal("30000").subtract(new BigDecimal(irps)).subtract(new BigDecimal(employeeInss)));
        return p;
    }
}
