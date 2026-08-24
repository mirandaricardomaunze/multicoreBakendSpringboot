package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.hr.dto.CreateTerminationRequest;
import mz.multicore.erp.modules.hr.dto.PayrollDeductionDTO;
import mz.multicore.erp.modules.hr.dto.TerminationDTO;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.SettlementStatus;
import mz.multicore.erp.modules.hr.model.Termination;
import mz.multicore.erp.modules.hr.model.TerminationReason;
import mz.multicore.erp.modules.hr.model.TerminationSettlementLine;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.TerminationRepository;
import mz.multicore.erp.modules.hr.repository.VacationRepository;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * B3 do harness do RH (RHC-35..40).
 *
 * <p>O que este bloco substitui é uma String: {@code changeEmployeeStatus(id, "TERMINATED")}, e
 * nada mais acontecia — o 13.º proporcional e o saldo de férias, que o sistema já sabia calcular,
 * nunca eram calculados nesta situação.
 */
class TerminationServiceTest {

    private static final Long COMPANY = 7L;
    private static final LocalDate SAIDA = LocalDate.of(2026, 6, 15);

    private TerminationRepository terminationRepository;
    private EmployeeRepository employeeRepository;
    private VacationRepository vacationRepository;
    private CompanyRepository companyRepository;
    private EmploymentContractService contractService;
    private SalaryHistoryService salaryHistoryService;
    private PayrollDeductionService payrollDeductionService;
    private HrPolicyService hrPolicyService;
    private DocumentNumberService documentNumberService;
    private FinanceService financeService;
    private AuditLogService auditLogService;
    private TerminationService service;

    @BeforeEach
    void setUp() {
        terminationRepository = mock(TerminationRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        vacationRepository = mock(VacationRepository.class);
        companyRepository = mock(CompanyRepository.class);
        contractService = mock(EmploymentContractService.class);
        salaryHistoryService = mock(SalaryHistoryService.class);
        payrollDeductionService = mock(PayrollDeductionService.class);
        hrPolicyService = mock(HrPolicyService.class);
        documentNumberService = mock(DocumentNumberService.class);
        financeService = mock(FinanceService.class);
        auditLogService = mock(AuditLogService.class);
        service = new TerminationService(terminationRepository, employeeRepository, vacationRepository,
                companyRepository, contractService, salaryHistoryService, payrollDeductionService,
                hrPolicyService, documentNumberService, financeService, auditLogService);

        Company company = new Company();
        company.setId(COMPANY);
        when(companyRepository.findById(COMPANY)).thenReturn(Optional.of(company));
        when(employeeRepository.findByIdAndCompanyId(5L, COMPANY)).thenReturn(Optional.of(employee()));
        when(salaryHistoryService.salaryOn(eq(5L), any())).thenReturn(Optional.of(new BigDecimal("30000")));
        when(vacationRepository.sumReservedDays(anyLong(), anyInt())).thenReturn(0);
        when(hrPolicyService.annualVacationDays(anyInt(), any())).thenReturn(Optional.empty());
        when(hrPolicyService.noticeDays(org.mockito.ArgumentMatchers.anyBoolean(), any()))
                .thenReturn(Optional.empty());
        when(payrollDeductionService.outstandingFor(5L)).thenReturn(List.of());
        when(contractService.findContractInPeriod(eq(5L), any(), any())).thenReturn(Optional.empty());
        when(documentNumberService.next(any())).thenReturn("AF-2026/1");
        when(terminationRepository.save(any(Termination.class))).thenAnswer(inv -> inv.getArgument(0));
        CurrentUserContext.setCurrentCompanyId(COMPANY);
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void settlement_hasSalaryUntilExit_thirteenthAndUnusedVacation() { // RHC-36
        // Estas três linhas o sistema já sabia calcular — só nunca as calculava nesta situação.
        TerminationDTO dto = service.preview(request(TerminationReason.INICIATIVA_TRABALHADOR, true));

        // Salário: 15 de 30 dias de Junho sobre 30.000 = 15.000
        assertTrue(hasLine(dto, "Salário de 06/2026", new BigDecimal("15000.00")));
        // 13.º: 6 meses de 12 sobre 30.000 = 15.000
        assertTrue(hasLine(dto, "13.º mês proporcional", new BigDecimal("15000.00")));
        // Férias: 22 dias por gozar × (30.000/30) = 22.000
        assertTrue(hasLine(dto, "Férias vencidas e não gozadas", new BigDecimal("22000.00")));
        assertEquals(0, new BigDecimal("52000.00").compareTo(dto.totalEarnings()));
    }

    @Test
    void withoutConfiguredVacationEntitlement_theSettlementSaysSo() { // §6
        // Um acerto que esconde o que não sabe calcular é pior do que um acerto incompleto que o diz.
        TerminationDTO dto = service.preview(request(TerminationReason.INICIATIVA_TRABALHADOR, true));

        assertTrue(dto.warnings().stream().anyMatch(w -> w.contains("valor histórico de 22 dias")));
        assertTrue(dto.warnings().stream().anyMatch(w -> w.contains("Valores Legais")));
    }

    @Test
    void withConfiguredEntitlement_thereIsNoWarningAndTheDaysAreTheConfiguredOnes() {
        when(hrPolicyService.annualVacationDays(anyInt(), any())).thenReturn(Optional.of(30));

        TerminationDTO dto = service.preview(request(TerminationReason.INICIATIVA_TRABALHADOR, true));

        assertTrue(hasLine(dto, "Férias vencidas e não gozadas", new BigDecimal("30000.00")));
        assertFalse(dto.warnings().stream().anyMatch(w -> w.contains("direito anual")));
    }

    @Test
    void unservedNotice_isOnlyDeductedWhenTheWorkerOwedIt() { // RHC-36
        // Descontar aviso prévio a quem foi despedido seria cobrar-lhe a decisão da empresa.
        when(hrPolicyService.noticeDays(eq(false), any())).thenReturn(Optional.of(30));

        TerminationDTO byWorker = service.preview(request(TerminationReason.INICIATIVA_TRABALHADOR, false));
        TerminationDTO byEmployer = service.preview(request(TerminationReason.INICIATIVA_EMPREGADOR, false));

        assertTrue(hasLine(byWorker, "Aviso prévio não cumprido", new BigDecimal("30000.00")));
        assertFalse(byEmployer.lines().stream().anyMatch(l -> l.description().contains("Aviso prévio")));
    }

    @Test
    void unservedNoticeWithoutConfiguredDays_deductsNothingAndSaysWhy() { // §6
        TerminationDTO dto = service.preview(request(TerminationReason.INICIATIVA_TRABALHADOR, false));

        assertFalse(dto.lines().stream().anyMatch(l -> l.description().contains("Aviso prévio")));
        assertTrue(dto.warnings().stream().anyMatch(w -> w.contains("aviso prévio")));
    }

    @Test
    void outstandingLoan_isDeductedInTheSettlement() { // RHC-37
        // Sem esta linha, quem sai a meio de um empréstimo levava o saldo consigo: o dinheiro tinha
        // saído da caixa e não voltava por porta nenhuma.
        when(payrollDeductionService.outstandingFor(5L)).thenReturn(List.of(outstandingLoan("8000")));

        TerminationDTO dto = service.preview(request(TerminationReason.INICIATIVA_TRABALHADOR, true));

        assertTrue(hasLine(dto, "Empréstimo por liquidar", new BigDecimal("8000")));
        assertEquals(0, new BigDecimal("8000").compareTo(dto.totalDeductions()));
        assertEquals(0, new BigDecimal("44000.00").compareTo(dto.netAmount()));
    }

    @Test
    void aSettlementCanBeNegative_andThatIsNotHidden() {
        // Quem sai a dever mais do que os proporcionais fica a dever à empresa. Esconder isso num
        // zero fingiria que a dívida desapareceu com a saída.
        when(payrollDeductionService.outstandingFor(5L)).thenReturn(List.of(outstandingLoan("90000")));

        TerminationDTO dto = service.preview(request(TerminationReason.INICIATIVA_TRABALHADOR, true));

        assertTrue(dto.netAmount().signum() < 0);
    }

    @Test
    void terminate_closesTheContract_andMarksTheEmployeeTerminated() { // RHC-35/39
        // O colaborador cessado deixa de poder ter recibos e férias — não por guarda nova, mas
        // porque findActiveEmployee já exige ACTIVE nas duas portas. O que faltava era chegar lá.
        service.terminate(request(TerminationReason.FIM_DO_TERMO, true));

        var captor = org.mockito.ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        assertEquals("TERMINATED", captor.getValue().getStatus());
        assertEquals(SAIDA, captor.getValue().getContractEndDate());
        verify(auditLogService).logCurrent(eq("TERMINATION_CREATE"), any());
    }

    @Test
    void terminatingTwice_isRefused() { // RHC-35
        when(terminationRepository.existsByCompanyIdAndEmployeeId(COMPANY, 5L)).thenReturn(true);

        var ex = assertThrows(BusinessRuleException.class,
                () -> service.terminate(request(TerminationReason.FIM_DO_TERMO, true)));
        assertTrue(ex.getMessage().contains("cessa-se uma vez"));
    }

    @Test
    void paySettlement_leavesTreasuryOnceAndIsAudited() { // RHC-38
        when(terminationRepository.findByIdWithLines(3L, COMPANY))
                .thenReturn(Optional.of(settlement("52000.00", SettlementStatus.POR_PAGAR)));

        TerminationDTO dto = service.paySettlement(3L);

        assertEquals("PAGO", dto.status());
        verify(financeService).registerAutoPayout(eq(new BigDecimal("52000.00")), any());
        verify(auditLogService).logCurrent(eq("TERMINATION_PAID"), any());
    }

    @Test
    void payingTwice_isRefused() { // RHC-38
        Termination paid = settlement("52000.00", SettlementStatus.PAGO);
        paid.setPaymentDate(LocalDate.of(2026, 6, 20));
        when(terminationRepository.findByIdWithLines(3L, COMPANY)).thenReturn(Optional.of(paid));

        var ex = assertThrows(BusinessRuleException.class, () -> service.paySettlement(3L));

        assertTrue(ex.getMessage().contains("já foi pago"));
        verify(financeService, never()).registerAutoPayout(any(), any());
    }

    @Test
    void aNegativeSettlementIsNotPaid() {
        when(terminationRepository.findByIdWithLines(3L, COMPANY))
                .thenReturn(Optional.of(settlement("-3000.00", SettlementStatus.POR_PAGAR)));

        var ex = assertThrows(BusinessRuleException.class, () -> service.paySettlement(3L));

        assertTrue(ex.getMessage().contains("continua a dever"));
        verify(financeService, never()).registerAutoPayout(any(), any());
    }

    @Test
    void employeeRole_cannotTerminate() {
        CurrentUserContext.setCurrentUser("operador", "EMPLOYEE");

        assertThrows(BusinessRuleException.class,
                () -> service.terminate(request(TerminationReason.FIM_DO_TERMO, true)));
        verify(terminationRepository, never()).save(any());
    }

    @Test
    void exitBeforeHiring_isRefused() {
        var ex = assertThrows(BusinessRuleException.class, () -> service.terminate(
                new CreateTerminationRequest(5L, LocalDate.of(2020, 1, 1), "FIM_DO_TERMO",
                        true, null, null)));
        assertTrue(ex.getMessage().contains("anterior à admissão"));
    }

    // ─── Apoio ────────────────────────────────────────────────────────────────

    private static boolean hasLine(TerminationDTO dto, String descriptionFragment, BigDecimal amount) {
        return dto.lines().stream()
                .anyMatch(l -> l.description().contains(descriptionFragment)
                        && l.amount().compareTo(amount) == 0);
    }

    private CreateTerminationRequest request(TerminationReason reason, boolean noticeServed) {
        return new CreateTerminationRequest(5L, SAIDA, reason.name(), noticeServed, null, null);
    }

    private PayrollDeductionDTO outstandingLoan(String outstanding) {
        return new PayrollDeductionDTO(9L, 5L, "Maria", "EMPRESTIMO", "Empréstimo",
                "Empréstimo de Março", new BigDecimal("12000"), new BigDecimal("2000"), 6,
                LocalDate.of(2026, 3, 1), null, new BigDecimal("4000"), new BigDecimal(outstanding),
                false, true, true, null);
    }

    private Termination settlement(String net, SettlementStatus status) {
        Company company = new Company();
        company.setId(COMPANY);
        Termination termination = new Termination();
        termination.setId(3L);
        termination.setCompany(company);
        termination.setEmployee(employee());
        termination.setSettlementNumber("AF-2026/1");
        termination.setTerminationDate(SAIDA);
        termination.setReason(TerminationReason.FIM_DO_TERMO);
        termination.setStatus(status);
        TerminationSettlementLine line = new TerminationSettlementLine();
        line.setDescription("Total");
        line.setAmount(new BigDecimal(net));
        line.setEarning(true);
        termination.addLine(line);
        termination.recalculateTotals();
        termination.setStatus(status);
        return termination;
    }

    private static Employee employee() {
        Employee employee = new Employee();
        employee.setId(5L);
        employee.setEmployeeNumber("EMP-5");
        employee.setName("Maria");
        employee.setStatus("ACTIVE");
        employee.setBaseSalary(new BigDecimal("30000"));
        employee.setHireDate(LocalDate.of(2024, 1, 15));
        return employee;
    }
}
