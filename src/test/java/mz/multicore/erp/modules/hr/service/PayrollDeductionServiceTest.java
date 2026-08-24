package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.hr.dto.CreatePayrollDeductionRequest;
import mz.multicore.erp.modules.hr.dto.PayrollDeductionDTO;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.PayrollDeduction;
import mz.multicore.erp.modules.hr.model.PayrollDeductionKind;
import mz.multicore.erp.modules.hr.model.Payslip;
import mz.multicore.erp.modules.hr.model.PayslipDeductionLine;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.PayrollDeductionRepository;
import mz.multicore.erp.modules.hr.repository.PayslipDeductionLineRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * B6 do harness do RH (RHC-60..63).
 *
 * <p>O caso que carrega a classe é o adiantamento: até aqui <b>saía da caixa e nunca voltava</b>,
 * porque nada o ligava ao recibo do período.
 */
class PayrollDeductionServiceTest {

    private static final Long COMPANY = 7L;
    private static final LocalDate AGOSTO_FIM = LocalDate.of(2026, 8, 31);

    private PayrollDeductionRepository deductionRepository;
    private PayslipDeductionLineRepository lineRepository;
    private EmployeeRepository employeeRepository;
    private CompanyRepository companyRepository;
    private FinanceService financeService;
    private AuditLogService auditLogService;
    private PayrollDeductionService service;

    @BeforeEach
    void setUp() {
        deductionRepository = mock(PayrollDeductionRepository.class);
        lineRepository = mock(PayslipDeductionLineRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        companyRepository = mock(CompanyRepository.class);
        financeService = mock(FinanceService.class);
        auditLogService = mock(AuditLogService.class);
        service = new PayrollDeductionService(deductionRepository, lineRepository,
                employeeRepository, companyRepository, financeService, auditLogService);

        Company company = new Company();
        company.setId(COMPANY);
        when(companyRepository.findById(COMPANY)).thenReturn(Optional.of(company));
        when(employeeRepository.findByIdAndCompanyId(5L, COMPANY)).thenReturn(Optional.of(employee()));
        when(deductionRepository.save(any(PayrollDeduction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(lineRepository.save(any(PayslipDeductionLine.class))).thenAnswer(inv -> inv.getArgument(0));
        when(lineRepository.sumApplied(anyLong())).thenReturn(BigDecimal.ZERO);
        CurrentUserContext.setCurrentCompanyId(COMPANY);
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void advance_leavesTreasuryImmediatelyAndIsAudited() { // RHC-60 — o 🔴 do bloco
        PayrollDeductionDTO dto = service.create(new CreatePayrollDeductionRequest(
                5L, "ADIANTAMENTO", "Adiantamento de Agosto", new BigDecimal("5000"),
                null, null, LocalDate.of(2026, 8, 10), null, null));

        verify(financeService).registerAutoPayout(eq(new BigDecimal("5000")), any());
        verify(auditLogService).logCurrent(eq("PAYROLL_DEDUCTION_CREATE"), any());
        assertTrue(dto.paidOut());
        assertEquals(1, dto.installments());
        assertEquals(0, new BigDecimal("5000").compareTo(dto.installmentAmount()));
    }

    @Test
    void advance_isDiscountedOnThePeriodPayslip() { // RHC-60 — a outra metade
        // Sair da caixa já saía. O que faltava era voltar.
        when(deductionRepository.findApplicable(5L, COMPANY, AGOSTO_FIM))
                .thenReturn(List.of(advance("5000")));

        BigDecimal total = service.applyTo(payslip(), new BigDecimal("25000"));

        assertEquals(0, new BigDecimal("5000").compareTo(total));
        verify(lineRepository).save(any(PayslipDeductionLine.class));
    }

    @Test
    void loan_takesOneInstallmentAndLeavesTheRestOwed() { // RHC-61
        PayrollDeduction loan = loan("12000", "2000", 6);
        when(deductionRepository.findApplicable(5L, COMPANY, AGOSTO_FIM)).thenReturn(List.of(loan));
        when(lineRepository.sumApplied(9L)).thenReturn(new BigDecimal("4000"));

        BigDecimal total = service.applyTo(payslip(), new BigDecimal("25000"));

        assertEquals(0, new BigDecimal("2000").compareTo(total));
        assertEquals(0, new BigDecimal("8000").compareTo(loan.outstanding(new BigDecimal("4000"))));
    }

    @Test
    void loan_lastInstallmentNeverGoesBelowZero() { // RHC-61
        // Prestação de 2.000 com 1.500 por liquidar: leva 1.500, não 2.000. Sem isto, o colaborador
        // acabaria a pagar mais do que pediu emprestado.
        PayrollDeduction loan = loan("12000", "2000", 6);
        when(deductionRepository.findApplicable(5L, COMPANY, AGOSTO_FIM)).thenReturn(List.of(loan));
        when(lineRepository.sumApplied(9L)).thenReturn(new BigDecimal("10500"));

        BigDecimal total = service.applyTo(payslip(), new BigDecimal("25000"));

        assertEquals(0, new BigDecimal("1500").compareTo(total));
    }

    @Test
    void settledLoan_isNotDiscountedAgain() { // RHC-61
        PayrollDeduction loan = loan("12000", "2000", 6);
        when(deductionRepository.findApplicable(5L, COMPANY, AGOSTO_FIM)).thenReturn(List.of(loan));
        when(lineRepository.sumApplied(9L)).thenReturn(new BigDecimal("12000"));

        assertEquals(0, BigDecimal.ZERO.compareTo(service.applyTo(payslip(), new BigDecimal("25000"))));
        verify(lineRepository, never()).save(any());
    }

    @Test
    void recurringDeduction_stopsAfterItsEndDate() { // RHC-62
        // A vigência é a consulta, não uma volta ao if: um desconto fora de prazo nem chega aqui.
        when(deductionRepository.findApplicable(5L, COMPANY, AGOSTO_FIM)).thenReturn(List.of());

        assertEquals(0, BigDecimal.ZERO.compareTo(service.applyTo(payslip(), new BigDecimal("25000"))));
    }

    @Test
    void deduction_neverTakesMoreThanTheSalaryLeaves() {
        // Não se pode tirar a um colaborador dinheiro que ele não recebeu. O que não couber
        // continua em dívida — não é erro nenhum, é o único desfecho honesto.
        when(deductionRepository.findApplicable(5L, COMPANY, AGOSTO_FIM))
                .thenReturn(List.of(advance("5000")));

        BigDecimal total = service.applyTo(payslip(), new BigDecimal("1200"));

        assertEquals(0, new BigDecimal("1200").compareTo(total));
    }

    @Test
    void twoDeductions_theOlderOneIsServedFirst() {
        // Com salário a menos, a ordem tem de ser previsível — senão o líquido do colaborador
        // dependia do que a base de dados devolvesse primeiro.
        PayrollDeduction older = advance("3000");
        older.setId(9L);
        older.setDescription("Adiantamento de Julho");
        PayrollDeduction newer = advance("3000");
        newer.setId(10L);
        newer.setDescription("Adiantamento de Agosto");
        when(deductionRepository.findApplicable(5L, COMPANY, AGOSTO_FIM))
                .thenReturn(List.of(older, newer));

        BigDecimal total = service.applyTo(payslip(), new BigDecimal("4000"));

        assertEquals(0, new BigDecimal("4000").compareTo(total));
        ArgumentCaptor<PayslipDeductionLine> captor = ArgumentCaptor.forClass(PayslipDeductionLine.class);
        verify(lineRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertEquals("Adiantamento de Julho", captor.getAllValues().get(0).getDescription());
        assertEquals(0, new BigDecimal("3000").compareTo(captor.getAllValues().get(0).getAmount()));
        assertEquals(0, new BigDecimal("1000").compareTo(captor.getAllValues().get(1).getAmount()));
    }

    @Test
    void cancellingAPayslip_putsTheInstallmentsBackInDebt() {
        // Sem isto, o empréstimo ficava pago com dinheiro que ninguém chegou a descontar.
        service.releaseFromPayslip(4L);

        verify(lineRepository).deleteByPayslipId(4L);
    }

    @Test
    void advanceWithSeveralInstallments_isRefusedAndSaysWhatToUse() {
        var ex = assertThrows(BusinessRuleException.class, () -> service.create(
                new CreatePayrollDeductionRequest(5L, "ADIANTAMENTO", "Adiantamento",
                        new BigDecimal("5000"), null, 3, LocalDate.of(2026, 8, 1), null, null)));

        assertTrue(ex.getMessage().contains("empréstimo"));
    }

    @Test
    void loanWithoutPrincipal_isRefused() {
        var ex = assertThrows(BusinessRuleException.class, () -> service.create(
                new CreatePayrollDeductionRequest(5L, "EMPRESTIMO", "Empréstimo", null,
                        new BigDecimal("2000"), 6, LocalDate.of(2026, 8, 1), null, null)));

        assertTrue(ex.getMessage().contains("valor entregue"));
    }

    @Test
    void recurringDeduction_doesNotLeaveTreasury() { // RHC-62
        // Um desconto recorrente não entrega dinheiro nenhum — pagá-lo seria dá-lo duas vezes.
        PayrollDeductionDTO dto = service.create(new CreatePayrollDeductionRequest(
                5L, "RECORRENTE", "Quota do sindicato", null, new BigDecimal("250"),
                null, LocalDate.of(2026, 1, 1), null, null));

        verify(financeService, never()).registerAutoPayout(any(), any());
        assertFalse(dto.paidOut());
        assertFalse(dto.settled(), "um recorrente sem capital nunca se salda sozinho");
    }

    @Test
    void employeeRole_cannotCreateDeductions() {
        CurrentUserContext.setCurrentUser("operador", "EMPLOYEE");

        assertThrows(BusinessRuleException.class, () -> service.create(
                new CreatePayrollDeductionRequest(5L, "ADIANTAMENTO", "Adiantamento",
                        new BigDecimal("5000"), null, null, LocalDate.of(2026, 8, 1), null, null)));
        verify(financeService, never()).registerAutoPayout(any(), any());
    }

    @Test
    void payslipLines_areDiscriminatedByKindAndDescription() { // RHC-63
        PayslipDeductionLine line = new PayslipDeductionLine();
        line.setId(3L);
        line.setDeduction(loan("12000", "2000", 6));
        line.setDescription("Empréstimo de Julho");
        line.setAmount(new BigDecimal("2000"));
        when(lineRepository.findByPayslipId(4L)).thenReturn(List.of(line));

        var lines = service.linesOf(4L);

        assertEquals(1, lines.size());
        assertEquals("Empréstimo", lines.get(0).kindLabel());
        assertEquals("Empréstimo de Julho", lines.get(0).description());
    }

    // ─── Apoio ────────────────────────────────────────────────────────────────

    private PayrollDeduction advance(String amount) {
        PayrollDeduction deduction = base(PayrollDeductionKind.ADIANTAMENTO, "Adiantamento");
        deduction.setPrincipalAmount(new BigDecimal(amount));
        deduction.setInstallmentAmount(new BigDecimal(amount));
        deduction.setInstallments(1);
        deduction.setPaidOut(true);
        return deduction;
    }

    private PayrollDeduction loan(String principal, String installment, int installments) {
        PayrollDeduction deduction = base(PayrollDeductionKind.EMPRESTIMO, "Empréstimo");
        deduction.setPrincipalAmount(new BigDecimal(principal));
        deduction.setInstallmentAmount(new BigDecimal(installment));
        deduction.setInstallments(installments);
        deduction.setPaidOut(true);
        return deduction;
    }

    private PayrollDeduction base(PayrollDeductionKind kind, String description) {
        Company company = new Company();
        company.setId(COMPANY);
        PayrollDeduction deduction = new PayrollDeduction();
        deduction.setId(9L);
        deduction.setCompany(company);
        deduction.setEmployee(employee());
        deduction.setKind(kind);
        deduction.setDescription(description);
        deduction.setStartDate(LocalDate.of(2026, 7, 1));
        deduction.setActive(true);
        return deduction;
    }

    private Payslip payslip() {
        Payslip p = new Payslip();
        p.setId(4L);
        p.setEmployee(employee());
        p.setYear(2026);
        p.setMonth(8);
        return p;
    }

    private static Employee employee() {
        Employee employee = new Employee();
        employee.setId(5L);
        employee.setEmployeeNumber("EMP-5");
        employee.setName("Maria");
        employee.setStatus("ACTIVE");
        employee.setBaseSalary(new BigDecimal("30000"));
        return employee;
    }
}
