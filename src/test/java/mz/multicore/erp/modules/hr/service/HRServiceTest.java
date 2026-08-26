package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.approvals.service.ApprovalService;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.hr.dto.CreateAbsenceRequest;
import mz.multicore.erp.modules.hr.dto.CreateExpenseClaimRequest;
import mz.multicore.erp.modules.hr.dto.CreatePayslipRequest;
import mz.multicore.erp.modules.hr.dto.CreateVacationRequest;
import mz.multicore.erp.modules.hr.dto.OvertimeValuationDTO;
import mz.multicore.erp.modules.hr.dto.PayrollCalculationDTO;
import mz.multicore.erp.modules.hr.dto.TimeEntryDTO;
import mz.multicore.erp.modules.hr.dto.UpsertEmployeeRequest;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.service.AppUserService;
import mz.multicore.erp.modules.hr.model.Absence;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.EmploymentContract;
import mz.multicore.erp.modules.hr.model.ExpenseClaim;
import mz.multicore.erp.modules.hr.model.Payslip;
import mz.multicore.erp.modules.hr.model.Vacation;
import mz.multicore.erp.modules.hr.repository.AbsenceRepository;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.ExpenseClaimRepository;
import mz.multicore.erp.modules.hr.repository.PayslipRepository;
import mz.multicore.erp.modules.hr.repository.VacationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HRServiceTest {

    private EmployeeRepository employeeRepository;
    private CompanyRepository companyRepository;
    private AuditLogService auditLogService;
    private VacationRepository vacationRepository;
    private PayslipRepository payslipRepository;
    private AbsenceRepository absenceRepository;
    private PayrollTaxService payrollTaxService;
    private DocumentNumberService documentNumberService;
    private ExpenseClaimRepository expenseClaimRepository;
    private AppUserService appUserService;
    private EmploymentContractService contractService;
    private TimeSheetService timeSheetService;
    private OvertimeValuationService overtimeValuationService;
    private SalaryHistoryService salaryHistoryService;
    private PayrollLiabilityService payrollLiabilityService;
    private PayrollDeductionService payrollDeductionService;
    private HrPolicyService hrPolicyService;
    private PayrollPeriodService payrollPeriodService;
    private FinanceService financeService;
    private ApplicationEventPublisher eventPublisher;
    private HRService service;

    @BeforeEach
    void setUp() {
        employeeRepository = mock(EmployeeRepository.class);
        companyRepository = mock(CompanyRepository.class);
        auditLogService = mock(AuditLogService.class);
        vacationRepository = mock(VacationRepository.class);
        payslipRepository = mock(PayslipRepository.class);
        absenceRepository = mock(AbsenceRepository.class);
        payrollTaxService = mock(PayrollTaxService.class);
        documentNumberService = mock(DocumentNumberService.class);
        expenseClaimRepository = mock(ExpenseClaimRepository.class);
        appUserService = mock(AppUserService.class);
        contractService = mock(EmploymentContractService.class);
        timeSheetService = mock(TimeSheetService.class);
        when(timeSheetService.workingDaysBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(3);
        overtimeValuationService = mock(OvertimeValuationService.class);
        salaryHistoryService = mock(SalaryHistoryService.class);
        payrollLiabilityService = mock(PayrollLiabilityService.class);
        payrollDeductionService = mock(PayrollDeductionService.class);
        hrPolicyService = mock(HrPolicyService.class);
        payrollPeriodService = mock(PayrollPeriodService.class);
        when(hrPolicyService.annualVacationDays(any(Integer.class), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(payrollDeductionService.applyTo(any(), any())).thenReturn(BigDecimal.ZERO);
        financeService = mock(FinanceService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new HRService(
                employeeRepository,
                expenseClaimRepository,
                payslipRepository,
                absenceRepository,
                vacationRepository,
                companyRepository,
                payrollTaxService,
                mock(ApprovalService.class),
                documentNumberService,
                auditLogService,
                financeService,
                appUserService,
                contractService,
                timeSheetService,
                overtimeValuationService,
                salaryHistoryService,
                payrollLiabilityService,
                payrollDeductionService,
                hrPolicyService,
                payrollPeriodService,
                eventPublisher
        );
        CurrentUserContext.setCurrentCompanyId(7L);
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void getAllEmployees_returnsOnlyActiveCompanyEmployees() {
        Employee employee = employee(1L, "EMP-1", "Ana");
        when(employeeRepository.findByCompanyIdOrderByName(7L)).thenReturn(List.of(employee));

        var result = service.getAllEmployees();

        assertEquals(1, result.size());
        assertEquals("Ana", result.get(0).name());
        verify(employeeRepository).findByCompanyIdOrderByName(7L);
    }

    @Test
    void createEmployee_assignsActiveCompany() {
        Company company = new Company();
        company.setId(7L);
        when(companyRepository.findById(7L)).thenReturn(Optional.of(company));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> {
            Employee employee = inv.getArgument(0);
            employee.setId(10L);
            return employee;
        });

        var result = service.createEmployee(request("EMP-10", "novo@empresa.test"));

        assertEquals("EMP-10", result.employeeNumber());
        assertEquals("ACTIVE", result.status());
        verify(auditLogService).logCurrent(eq("EMPLOYEE_CREATE"), any());
    }

    @Test
    void createEmployee_preservesOptionalPhotoInTheCardAndDto() {
        Company company = new Company();
        company.setId(7L);
        when(companyRepository.findById(7L)).thenReturn(Optional.of(company));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> {
            Employee employee = inv.getArgument(0);
            employee.setId(11L);
            return employee;
        });
        byte[] photo = {1, 2, 3, 4};
        UpsertEmployeeRequest base = request("EMP-11", "foto@empresa.test");
        var withPhoto = new UpsertEmployeeRequest(
                base.employeeNumber(), base.name(), base.email(), base.phone(), photo, base.taxId(),
                base.inssNumber(), base.dependentsCount(), base.department(), base.role(),
                base.baseSalary(), base.hireDate(), base.contractEndDate(), base.username(),
                base.bankName(), base.bankAccount());

        var result = service.createEmployee(withPhoto);

        assertArrayEquals(photo, result.photo());
    }

    @Test
    void employeeRole_cannotCreateEmployee() {
        CurrentUserContext.setCurrentUser("operador", "EMPLOYEE");

        assertThrows(BusinessRuleException.class,
                () -> service.createEmployee(request("EMP-10", "novo@empresa.test")));
    }

    @Test
    void updateEmployee_rejectsEmployeeFromAnotherCompany() {
        when(employeeRepository.findByIdAndCompanyId(99L, 7L)).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class,
                () -> service.updateEmployee(99L, request("EMP-99", "outro@empresa.test")));
    }

    @Test
    void submitVacation_aboveAnnualBalance_isBlocked() {
        Employee active = employee(5L, "EMP-5", "Ferias");
        when(employeeRepository.findByIdAndCompanyId(5L, 7L)).thenReturn(Optional.of(active));
        // Direito anual 22; já reservados 20 → restam 2; pedir 6 dias (01→06) excede o saldo.
        when(vacationRepository.sumReservedDays(5L, 2026)).thenReturn(20);

        var ex = assertThrows(BusinessRuleException.class, () -> service.submitVacation(
                new CreateVacationRequest(5L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 6), 2026, null)));
        assertEquals(true, ex.getMessage().contains("Saldo de férias insuficiente"));
    }

    @Test
    void decideVacation_employeeRole_isBlocked() {
        CurrentUserContext.setCurrentUser("operador", "EMPLOYEE");

        assertThrows(BusinessRuleException.class,
                () -> service.decideVacation(1L, true, null));
    }

    @Test
    void createPayslip_withUnjustifiedAbsence_deductsFromNet() {
        Employee emp = employee(8L, "EMP-8", "Faltoso");
        emp.setBaseSalary(new BigDecimal("30000")); // valor/dia = 30000/30 = 1000
        when(employeeRepository.findByIdAndCompanyId(8L, 7L)).thenReturn(Optional.of(emp));
        when(payslipRepository.findByEmployeeIdAndYearAndMonth(8L, 2026, 6)).thenReturn(Optional.empty());
        when(documentNumberService.next(any())).thenReturn("REC-2026/1");
        // 3 dias de falta injustificada dentro de Junho → desconto de 3 × 1000 = 3000
        Absence ab = new Absence();
        ab.setStartDate(LocalDate.of(2026, 6, 10));
        ab.setEndDate(LocalDate.of(2026, 6, 12));
        when(absenceRepository.findUnpaidOverlapping(eq(8L), any(), any(), any())).thenReturn(List.of(ab));
        // Impostos a zero para isolar o desconto por faltas.
        when(payrollTaxService.calculate(any(), any(), any(), eq(2026), eq(6)))
                .thenReturn(new PayrollCalculationDTO(
                        new BigDecimal("30000"), new BigDecimal("30000"),
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("30000"),
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "cfg", "lei"));
        when(payslipRepository.save(any(Payslip.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = service.createPayslip(new CreatePayslipRequest(
                8L, 2026, 6, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null));

        assertEquals(0, new BigDecimal("3000.00").compareTo(dto.absenceDeduction()));
        assertEquals(0, new BigDecimal("27000.00").compareTo(dto.netPay())); // 30000 − 3000
    }

    // ─── B7.1: guardas de perfil e rasto de auditoria (RHC-01..05) ─────────────
    // Antes destas guardas, qualquer utilizador autenticado da empresa lançava faltas, apagava faltas
    // e pedia férias em nome de um colega — o employeeId vem do corpo do pedido.

    @Test
    void recordAbsence_employeeRole_isBlocked() { // RHC-01
        CurrentUserContext.setCurrentUser("operador", "EMPLOYEE");

        var ex = assertThrows(BusinessRuleException.class, () -> service.recordAbsence(
                new CreateAbsenceRequest(5L, "UNJUSTIFIED",
                        LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2), "sem aviso", false)));
        assertEquals(true, ex.getMessage().contains("gestores ou administradores"));
        verify(absenceRepository, never()).save(any(Absence.class));
    }

    @Test
    void recordAbsence_asManager_isAudited() { // RHC-05
        Employee emp = employee(5L, "EMP-5", "Faltoso");
        when(employeeRepository.findByIdAndCompanyId(5L, 7L)).thenReturn(Optional.of(emp));
        when(absenceRepository.save(any(Absence.class))).thenAnswer(inv -> inv.getArgument(0));

        service.recordAbsence(new CreateAbsenceRequest(5L, "UNJUSTIFIED",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2), "sem aviso", false));

        verify(auditLogService).logCurrent(eq("ABSENCE_CREATE"), any());
    }

    @Test
    void deleteAbsence_employeeRole_isBlocked() { // RHC-02
        // A falta existe e é da empresa activa: sem a guarda, esta chamada eliminava-a.
        Absence absence = new Absence();
        absence.setEmployee(employee(5L, "EMP-5", "Faltoso"));
        absence.setAbsenceType("UNJUSTIFIED");
        absence.setStartDate(LocalDate.of(2026, 6, 1));
        absence.setEndDate(LocalDate.of(2026, 6, 2));
        when(absenceRepository.findByIdAndEmployeeCompanyId(3L, 7L)).thenReturn(Optional.of(absence));
        when(absenceRepository.existsByIdAndEmployeeCompanyId(3L, 7L)).thenReturn(true);
        CurrentUserContext.setCurrentUser("operador", "EMPLOYEE");

        var ex = assertThrows(BusinessRuleException.class, () -> service.deleteAbsence(3L));
        assertEquals(true, ex.getMessage().contains("gestores ou administradores"));
        verify(absenceRepository, never()).deleteByIdAndEmployeeCompanyId(any(), any());
    }

    @Test
    void deleteAbsence_asManager_isAudited() { // RHC-05
        Absence absence = new Absence();
        absence.setEmployee(employee(5L, "EMP-5", "Faltoso"));
        absence.setAbsenceType("UNJUSTIFIED");
        absence.setStartDate(LocalDate.of(2026, 6, 1));
        absence.setEndDate(LocalDate.of(2026, 6, 2));
        absence.setTotalDays(2);
        when(absenceRepository.findByIdAndEmployeeCompanyId(3L, 7L)).thenReturn(Optional.of(absence));

        service.deleteAbsence(3L);

        verify(absenceRepository).deleteByIdAndEmployeeCompanyId(3L, 7L);
        verify(auditLogService).logCurrent(eq("ABSENCE_DELETE"), any());
    }

    // ─── B7.2: "o próprio" passa a ser identificável (RHC-03/04/06) ────────────
    // A ligação Employee↔AppUser (V48) devolve o self-service sem reabrir o furo: agir por outro
    // deixa de ser indistinguível de agir por si.

    @Test
    void submitVacation_employeeForSelf_isAllowed() { // RHC-03
        Employee self = employee(5L, "EMP-5", "Maria");
        when(employeeRepository.findByIdAndCompanyId(5L, 7L)).thenReturn(Optional.of(self));
        when(employeeRepository.findByCompanyIdAndAppUserUsername(7L, "maria")).thenReturn(Optional.of(self));
        when(vacationRepository.sumReservedDays(5L, 2026)).thenReturn(0);
        when(vacationRepository.save(any(Vacation.class))).thenAnswer(inv -> inv.getArgument(0));
        CurrentUserContext.setCurrentUser("maria", "EMPLOYEE");

        var dto = service.submitVacation(
                new CreateVacationRequest(5L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3), 2026, null));

        assertEquals("PENDING", dto.status());
        verify(vacationRepository).save(any(Vacation.class));
    }

    @Test
    void submitVacation_employeeForColleague_isBlocked() { // RHC-03
        Employee colleague = employee(5L, "EMP-5", "Colega");
        Employee self = employee(9L, "EMP-9", "Maria");
        when(employeeRepository.findByIdAndCompanyId(5L, 7L)).thenReturn(Optional.of(colleague));
        when(employeeRepository.findByCompanyIdAndAppUserUsername(7L, "maria")).thenReturn(Optional.of(self));
        when(vacationRepository.sumReservedDays(5L, 2026)).thenReturn(0);
        CurrentUserContext.setCurrentUser("maria", "EMPLOYEE");

        var ex = assertThrows(BusinessRuleException.class, () -> service.submitVacation(
                new CreateVacationRequest(5L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3), 2026, null)));
        assertEquals(true, ex.getMessage().contains("em seu próprio nome"));
        verify(vacationRepository, never()).save(any());
    }

    @Test
    void submitVacation_accountNotLinkedToEmployee_saysWhatToDo() { // RHC-03
        when(employeeRepository.findByIdAndCompanyId(5L, 7L))
                .thenReturn(Optional.of(employee(5L, "EMP-5", "Colega")));
        when(employeeRepository.findByCompanyIdAndAppUserUsername(7L, "avulso")).thenReturn(Optional.empty());
        CurrentUserContext.setCurrentUser("avulso", "EMPLOYEE");

        var ex = assertThrows(BusinessRuleException.class, () -> service.submitVacation(
                new CreateVacationRequest(5L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3), 2026, null)));
        // A recusa tem de dizer o que fazer a seguir, não só que falhou.
        assertEquals(true, ex.getMessage().contains("Peça ao RH"));
    }

    @Test
    void submitExpense_employeeForColleague_isBlocked() { // RHC-04
        Employee colleague = employee(5L, "EMP-5", "Colega");
        Employee self = employee(9L, "EMP-9", "Maria");
        when(employeeRepository.findByIdAndCompanyId(5L, 7L)).thenReturn(Optional.of(colleague));
        when(employeeRepository.findByCompanyIdAndAppUserUsername(7L, "maria")).thenReturn(Optional.of(self));
        CurrentUserContext.setCurrentUser("maria", "EMPLOYEE");

        var ex = assertThrows(BusinessRuleException.class, () -> service.submitExpense(
                new CreateExpenseClaimRequest(5L, new BigDecimal("180.00"), "TRAVEL", "Deslocação a Nampula")));
        assertEquals(true, ex.getMessage().contains("em seu próprio nome"));
        verify(expenseClaimRepository, never()).save(any(ExpenseClaim.class));
    }

    @Test
    void submitExpense_employeeForSelf_isAllowedAndAudited() { // RHC-04
        Employee self = employee(5L, "EMP-5", "Maria");
        when(employeeRepository.findByIdAndCompanyId(5L, 7L)).thenReturn(Optional.of(self));
        when(employeeRepository.findByCompanyIdAndAppUserUsername(7L, "maria")).thenReturn(Optional.of(self));
        when(expenseClaimRepository.save(any(ExpenseClaim.class))).thenAnswer(inv -> inv.getArgument(0));
        CurrentUserContext.setCurrentUser("maria", "EMPLOYEE");

        service.submitExpense(new CreateExpenseClaimRequest(
                5L, new BigDecimal("180.00"), "TRAVEL", "Deslocação a Nampula"));

        ArgumentCaptor<String> detail = ArgumentCaptor.forClass(String.class);
        verify(auditLogService).logCurrent(eq("EXPENSE_SUBMIT"), detail.capture());
        // O rasto continua a dizer as duas coisas: de quem é a despesa e quem a submeteu.
        assertEquals(true, detail.getValue().contains("Maria"));
        assertEquals(true, detail.getValue().contains("maria"));
    }

    @Test
    void getAllPayslips_employeeSeesOnlyOwn() { // RHC-06
        Employee self = employee(5L, "EMP-5", "Maria");
        when(employeeRepository.findByCompanyIdAndAppUserUsername(7L, "maria")).thenReturn(Optional.of(self));
        when(payslipRepository.findAllWithEmployeeByCompanyId(7L))
                .thenReturn(List.of(payslip(self), payslip(employee(9L, "EMP-9", "Colega"))));
        CurrentUserContext.setCurrentUser("maria", "EMPLOYEE");

        var result = service.getAllPayslips();

        assertEquals(1, result.size());
        assertEquals("Maria", result.get(0).employeeName());
    }

    @Test
    void getAllPayslips_managerSeesEveryone() { // RHC-06
        when(payslipRepository.findAllWithEmployeeByCompanyId(7L)).thenReturn(List.of(
                payslip(employee(5L, "EMP-5", "Maria")), payslip(employee(9L, "EMP-9", "Colega"))));

        assertEquals(2, service.getAllPayslips().size());
    }

    @Test
    void loadPayslipForPrint_colleaguePayslip_isBlocked() { // RHC-06
        Employee colleague = employee(9L, "EMP-9", "Colega");
        when(payslipRepository.findByIdWithEmployeeAndCompanyId(3L, 7L))
                .thenReturn(Optional.of(payslip(colleague)));
        when(employeeRepository.findByCompanyIdAndAppUserUsername(7L, "maria"))
                .thenReturn(Optional.of(employee(5L, "EMP-5", "Maria")));
        CurrentUserContext.setCurrentUser("maria", "EMPLOYEE");

        var ex = assertThrows(BusinessRuleException.class, () -> service.loadPayslipForPrint(3L));
        assertEquals(true, ex.getMessage().contains("em seu próprio nome"));
    }

    @Test
    void linkingUserAlreadyLinkedToAnotherEmployee_isBlocked() { // RHC-04 (unicidade da V48)
        Company company = new Company();
        company.setId(7L);
        when(companyRepository.findById(7L)).thenReturn(Optional.of(company));
        AppUser user = new AppUser();
        user.setUsername("maria");
        user.grantCompany(company, "EMPLOYEE");
        when(appUserService.findByUsername("maria")).thenReturn(user);
        when(employeeRepository.existsByCompanyIdAndAppUserUsername(7L, "maria")).thenReturn(true);

        var ex = assertThrows(BusinessRuleException.class,
                () -> service.createEmployee(requestWithUser("EMP-20", "novo@empresa.test", "maria")));
        assertEquals(true, ex.getMessage().contains("já está associado a outro colaborador"));
    }

    @Test
    void linkingUserWithoutAccessToActiveCompany_isBlocked() {
        Company company = new Company();
        company.setId(7L);
        when(companyRepository.findById(7L)).thenReturn(Optional.of(company));
        AppUser outsider = new AppUser();
        outsider.setUsername("externo");
        when(appUserService.findByUsername("externo")).thenReturn(outsider);

        var ex = assertThrows(BusinessRuleException.class,
                () -> service.createEmployee(requestWithUser("EMP-21", "outro@empresa.test", "externo")));
        assertEquals(true, ex.getMessage().contains("não tem acesso a esta empresa"));
    }

    // ─── B1: o contrato manda na folha ────────────────────────────────────────

    @Test
    void monthlyPayroll_skipsEmployeeWithoutCurrentContract_andSaysWho() {
        // O furo que este bloco fecha: quem tem contrato terminado a 31 de Julho recebia recibo em
        // Agosto, em silêncio, com saída de tesouraria e tudo.
        Employee comContrato = employee(5L, "EMP-5", "Com Contrato");
        Employee semContrato = employee(9L, "EMP-9", "Sem Contrato");
        when(employeeRepository.findByCompanyIdOrderByName(7L)).thenReturn(List.of(comContrato, semContrato));
        when(payslipRepository.findByEmployeeIdAndYearAndMonth(any(), eq(2026), eq(8)))
                .thenReturn(Optional.empty());
        when(contractService.findContractInPeriod(eq(5L), any(), any()))
                .thenReturn(Optional.of(new EmploymentContract()));
        when(contractService.findContractInPeriod(eq(9L), any(), any())).thenReturn(Optional.empty());
        stubPayslipCreation(comContrato);

        var run = service.processMonthlyPayroll(2026, 8);

        assertEquals(1, run.generated().size());
        assertEquals(1, run.skipped().size());
        assertEquals("Sem Contrato", run.skipped().get(0).employeeName());
        assertEquals(true, run.skipped().get(0).reason().contains("Sem contrato vigente em 8/2026"));
        // O silêncio era o problema: a mensagem ao operador tem de nomear quem ficou de fora.
        assertEquals(true, run.summaryMessage().contains("Sem Contrato"));
    }

    @Test
    void monthlyPayroll_inactiveEmployeeIsNotReportedAsSkipped() {
        // Inactivo é decisão já tomada e visível na ficha — listá-lo como "não processado" seria ruído.
        Employee inactivo = employee(9L, "EMP-9", "Saiu");
        inactivo.setStatus("TERMINATED");
        when(employeeRepository.findByCompanyIdOrderByName(7L)).thenReturn(List.of(inactivo));

        var run = service.processMonthlyPayroll(2026, 8);

        assertEquals(0, run.generated().size());
        assertEquals(0, run.skipped().size());
    }

    // ─── B4: o recibo de Março usa o salário de Março ─────────────────────────

    @Test
    void payslip_usesTheSalaryInForceForThatPeriod() { // RHC-46
        // Reprocessar Março depois de um aumento em Junho pagava, até agora, ao valor de Junho —
        // e nada parecia errado, porque o número era perfeitamente normal.
        Employee emp = employee(8L, "EMP-8", "Aumentado");
        emp.setBaseSalary(new BigDecimal("35000")); // valor de HOJE, depois do aumento
        stubPayslipCreation(emp, 3);
        when(salaryHistoryService.salaryOn(eq(8L), eq(LocalDate.of(2026, 3, 31))))
                .thenReturn(Optional.of(new BigDecimal("30000")));

        var dto = service.createPayslip(new CreatePayslipRequest(
                8L, 2026, 3, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null));

        assertEquals(0, new BigDecimal("30000").compareTo(dto.baseSalary()),
                "o recibo de Março paga ao valor de Março");
    }

    @Test
    void payslip_withoutSalaryHistory_fallsBackToTheEmployeeCard() {
        // Colaboradores anteriores a este bloco continuam a comportar-se como sempre.
        Employee emp = employee(8L, "EMP-8", "Antigo");
        emp.setBaseSalary(new BigDecimal("27000"));
        stubPayslipCreation(emp, 3);
        when(salaryHistoryService.salaryOn(eq(8L), any())).thenReturn(Optional.empty());

        var dto = service.createPayslip(new CreatePayslipRequest(
                8L, 2026, 3, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null));

        assertEquals(0, new BigDecimal("27000").compareTo(dto.baseSalary()));
    }

    @Test
    void updateEmployee_cannotChangeSalaryThroughTheCard() { // RHC-45
        // A ficha deixou de ser a porta: sobrepor perdia o valor anterior, o autor, o motivo e a data.
        Employee existing = employee(9L, "EMP-9", "Maria");
        existing.setBaseSalary(new BigDecimal("30000"));
        when(employeeRepository.findByIdAndCompanyId(9L, 7L)).thenReturn(Optional.of(existing));

        var ex = assertThrows(BusinessRuleException.class, () -> service.updateEmployee(9L,
                new UpsertEmployeeRequest("EMP-9", "Maria", "maria@empresa.test", null, null, null, null, 0,
                        "RH", "EMPLOYEE", new BigDecimal("35000"), LocalDate.of(2026, 1, 1), null, null, null, null)));
        assertEquals(true, ex.getMessage().contains("Evolução Salarial"),
                "a recusa diz onde se faz");
    }

    // ─── B2.2: o recibo lê as horas extra do ponto fechado ────────────────────

    @Test
    void payslip_withClosedTimeSheet_readsOvertimeFromIt() { // RHC-26
        // O defeito que este bloco fecha: o valor era digitado à mão, sem origem e sem contestação.
        Employee emp = employee(8L, "EMP-8", "Extra");
        emp.setBaseSalary(new BigDecimal("30000"));
        stubPayslipCreation(emp, 6);
        when(timeSheetService.isPeriodClosed(2026, 6)).thenReturn(true);
        when(overtimeValuationService.valueFor(eq(8L), eq(2026), eq(6), any()))
                .thenReturn(Optional.of(new OvertimeValuationDTO(
                        new BigDecimal("4.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                        new BigDecimal("1250.00"), "Acréscimos 2026", "Confirmado com contabilista")));

        var dto = service.createPayslip(new CreatePayslipRequest(
                8L, 2026, 6, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null));

        assertEquals(0, new BigDecimal("1250.00").compareTo(dto.overtime()),
                "o valor vem do ponto, não da caixa");
    }

    @Test
    void payslip_manualOvertimeDivergingFromClosedSheet_requiresJustification() { // RHC-30
        Employee emp = employee(8L, "EMP-8", "Extra");
        emp.setBaseSalary(new BigDecimal("30000"));
        stubPayslipCreation(emp, 6);
        when(timeSheetService.isPeriodClosed(2026, 6)).thenReturn(true);
        when(overtimeValuationService.valueFor(eq(8L), eq(2026), eq(6), any()))
                .thenReturn(Optional.of(new OvertimeValuationDTO(
                        new BigDecimal("4.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                        new BigDecimal("1250.00"), "Acréscimos 2026", null)));

        var ex = assertThrows(BusinessRuleException.class, () -> service.createPayslip(
                new CreatePayslipRequest(8L, 2026, 6, BigDecimal.ZERO, new BigDecimal("9000"),
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null)));
        assertEquals(true, ex.getMessage().contains("justificação"));
    }

    @Test
    void payslip_manualOvertimeWithJustification_isAllowedAndAudited() { // RHC-30
        // A porta que era a regra passa a ser a excepção — mas continua a existir, e com nome.
        Employee emp = employee(8L, "EMP-8", "Extra");
        emp.setBaseSalary(new BigDecimal("30000"));
        stubPayslipCreation(emp, 6);
        when(timeSheetService.isPeriodClosed(2026, 6)).thenReturn(true);
        when(overtimeValuationService.valueFor(eq(8L), eq(2026), eq(6), any()))
                .thenReturn(Optional.of(new OvertimeValuationDTO(
                        new BigDecimal("4.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                        new BigDecimal("1250.00"), "Acréscimos 2026", null)));

        var dto = service.createPayslip(new CreatePayslipRequest(
                8L, 2026, 6, BigDecimal.ZERO, new BigDecimal("9000"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null,
                "Acordo de regularização de Maio"));

        assertEquals(0, new BigDecimal("9000").compareTo(dto.overtime()));
        verify(auditLogService).logCurrent(eq("PAYSLIP_OVERTIME_OVERRIDE"), any());
    }

    @Test
    void payslip_withoutClosedTimeSheet_keepsManualValue() {
        // Sem ponto fechado o comportamento é o de sempre: uma loja que não usa o módulo continua
        // a emitir recibos avulsos.
        Employee emp = employee(8L, "EMP-8", "Extra");
        emp.setBaseSalary(new BigDecimal("30000"));
        stubPayslipCreation(emp, 6);
        when(timeSheetService.isPeriodClosed(2026, 6)).thenReturn(false);

        var dto = service.createPayslip(new CreatePayslipRequest(
                8L, 2026, 6, BigDecimal.ZERO, new BigDecimal("500"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null));

        assertEquals(0, new BigDecimal("500").compareTo(dto.overtime()));
    }

    @Test
    void monthlyPayroll_withOpenTimeSheetThatHasEntries_isBlocked() { // RHC-27
        when(timeSheetService.getEntries(2026, 8)).thenReturn(List.of(mock(TimeEntryDTO.class)));
        when(timeSheetService.isPeriodClosed(2026, 8)).thenReturn(false);

        var ex = assertThrows(BusinessRuleException.class, () -> service.processMonthlyPayroll(2026, 8));
        assertEquals(true, ex.getMessage().contains("ainda está aberta"));
    }

    @Test
    void monthlyPayroll_withoutAnyTimeEntries_isNotBlocked() { // RHC-27
        // Uma empresa que não usa o ponto não pode ficar sem processar salários por causa de um
        // fecho que não lhe diz respeito.
        when(timeSheetService.getEntries(2026, 8)).thenReturn(List.of());
        when(employeeRepository.findByCompanyIdOrderByName(7L)).thenReturn(List.of());

        assertEquals(0, service.processMonthlyPayroll(2026, 8).generated().size());
    }

    @Test
    void justifyAbsence_changesTypeAndIsAudited() { // RHC-25
        Absence pending = new Absence();
        pending.setEmployee(employee(5L, "EMP-5", "Maria"));
        pending.setAbsenceType("PENDING_JUSTIFICATION");
        pending.setStartDate(LocalDate.of(2026, 6, 10));
        pending.setEndDate(LocalDate.of(2026, 6, 10));
        pending.setTotalDays(1);
        when(absenceRepository.findByIdAndEmployeeCompanyId(3L, 7L)).thenReturn(Optional.of(pending));
        when(absenceRepository.save(any(Absence.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = service.justifyAbsence(3L, "SICK", "Atestado médico", true);

        assertEquals("SICK", dto.absenceType());
        verify(auditLogService).logCurrent(eq("ABSENCE_JUSTIFY"), any());
    }

    @Test
    void justifyAbsence_withoutReason_isBlocked() { // RHC-25
        var ex = assertThrows(BusinessRuleException.class,
                () -> service.justifyAbsence(3L, "SICK", "  ", false));
        assertEquals(true, ex.getMessage().contains("motivo"));
    }

    // ─── B5: pagar a folha passa a registar a dívida ao Estado ────────────────

    @Test
    void markPayslipPaid_raisesTheStateLiabilities() { // RHC-50
        // Antes disto só o líquido saía. O IRPS retido e o INSS das duas partes ficavam na conta da
        // empresa indistinguíveis de dinheiro próprio, sem obrigação, sem prazo e sem aviso.
        when(payslipRepository.findByIdWithEmployeeAndCompanyId(4L, 7L))
                .thenReturn(Optional.of(approvedPayslipToPay()));
        when(payslipRepository.save(any(Payslip.class))).thenAnswer(inv -> inv.getArgument(0));

        service.markPayslipPaid(4L);

        verify(payrollLiabilityService).accrueForPeriod(2026, 6);
    }

    @Test
    void markPayslipPaid_publishesTheEventThatFeedsAccounting() { // RHC-53/54
        // Por evento, e não por chamada directa: o RH não pode passar a conhecer a contabilidade só
        // para a manter informada — é a mesma decisão do SaleRegisteredEvent no comercial.
        when(payslipRepository.findByIdWithEmployeeAndCompanyId(4L, 7L))
                .thenReturn(Optional.of(approvedPayslipToPay()));
        when(payslipRepository.save(any(Payslip.class))).thenAnswer(inv -> inv.getArgument(0));

        service.markPayslipPaid(4L);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        var event = (mz.multicore.erp.architecture.events.PayslipPaidEvent) captor.getValue();
        assertEquals(0, new BigDecimal("31000").compareTo(event.grossPay())); // base + subsídios + extra
        assertEquals(0, new BigDecimal("900").compareTo(event.employerInss()));
        assertEquals(0, new BigDecimal("26100").compareTo(event.netPay()));
    }

    // ─── B6: adiantamentos e prestações entram no recibo ──────────────────────

    @Test
    void payslip_addsCommittedDeductionsOnTopOfTheManualOne() { // RHC-60/61
        // O adiantamento saía da caixa e nunca voltava. Agora volta pelo recibo do período, e o
        // valor manual continua a existir por cima — são coisas diferentes.
        Employee emp = employee(5L, "EMP-5", "Maria");
        emp.setBaseSalary(new BigDecimal("30000"));
        stubPayslipCreation(emp);
        when(salaryHistoryService.salaryOn(eq(5L), any())).thenReturn(Optional.of(new BigDecimal("30000")));
        when(payrollDeductionService.applyTo(any(), any())).thenReturn(new BigDecimal("5000"));

        var dto = service.createPayslip(new CreatePayslipRequest(
                5L, 2026, 8, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("500"), null, null));

        assertEquals(0, new BigDecimal("5500").compareTo(dto.otherDeductions()));
        assertEquals(0, new BigDecimal("24500").compareTo(dto.netPay()));
    }

    @Test
    void cancelPayslip_putsTheInstallmentsBackInDebt() { // RHC-61
        // Sem isto, o empréstimo ficava pago com dinheiro que ninguém chegou a descontar.
        Payslip draft = payslip(employee(5L, "EMP-5", "Maria"));
        draft.setId(4L);
        draft.setStatus("DRAFT");
        when(payslipRepository.findByIdWithEmployeeAndCompanyId(4L, 7L)).thenReturn(Optional.of(draft));
        when(payslipRepository.save(any(Payslip.class))).thenAnswer(inv -> inv.getArgument(0));

        service.cancelPayslip(4L);

        verify(payrollDeductionService).releaseFromPayslip(4L);
    }

    // ─── B8: correcções ao que já existia ─────────────────────────────────────

    @Test
    void approvePayslip_movesDraftToApproved_andIsAudited() { // RHC-72
        Payslip draft = payslip(employee(5L, "EMP-5", "Maria"));
        draft.setStatus("DRAFT");
        draft.setNetPay(new BigDecimal("26100"));
        when(payslipRepository.findByIdWithEmployeeAndCompanyId(4L, 7L)).thenReturn(Optional.of(draft));
        when(payslipRepository.save(any(Payslip.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = service.approvePayslip(4L);

        assertEquals("APPROVED", dto.status());
        verify(auditLogService).logCurrent(eq("PAYSLIP_APPROVE"), any());
    }

    @Test
    void payingAPayslipThatWasNotApproved_isRefused() { // RHC-72
        // A HR_PAYROLL_SPEC §3 prometia DRAFT → APPROVED → PAID e o recibo só conhecia DRAFT e PAID:
        // quem processava a folha pagava-a sozinho, sem segunda vista sobre os números.
        Payslip draft = payslip(employee(5L, "EMP-5", "Maria"));
        draft.setStatus("DRAFT");
        when(payslipRepository.findByIdWithEmployeeAndCompanyId(4L, 7L)).thenReturn(Optional.of(draft));

        var ex = assertThrows(BusinessRuleException.class, () -> service.markPayslipPaid(4L));

        assertEquals(true, ex.getMessage().contains("Aprove-o primeiro"));
        verify(financeService, never()).registerAutoPayout(any(), any());
    }

    @Test
    void vacation_countsWorkingDays_notCalendarDays() { // RHC-70
        // Quem pedia 22 dias seguidos gastava o ano inteiro; quem partia as férias em bocados saía a
        // ganhar. Duas contas diferentes para o mesmo direito.
        Employee emp = employee(5L, "EMP-5", "Maria");
        when(employeeRepository.findByIdAndCompanyId(5L, 7L)).thenReturn(Optional.of(emp));
        // Sexta a segunda: 4 dias de calendário, 2 úteis.
        when(timeSheetService.workingDaysBetween(LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 8)))
                .thenReturn(2);
        when(vacationRepository.sumReservedDays(5L, 2026)).thenReturn(0);
        when(vacationRepository.save(any(Vacation.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = service.submitVacation(new CreateVacationRequest(
                5L, LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 8), 2026, null));

        assertEquals(2, dto.totalDays());
    }

    @Test
    void vacation_usesTheConfiguredAnnualEntitlement() { // RHC-71
        // O direito anual era a constante 22 compilada no serviço — não era da empresa, não era do
        // contrato, não era da lei. Passou a vir da configuração, e 22 é só o último recurso.
        Employee emp = employee(5L, "EMP-5", "Maria");
        emp.setHireDate(LocalDate.of(2020, 1, 1));
        when(employeeRepository.findByIdAndCompanyId(5L, 7L)).thenReturn(Optional.of(emp));
        when(hrPolicyService.annualVacationDays(any(Integer.class), any(LocalDate.class)))
                .thenReturn(Optional.of(30));
        when(timeSheetService.workingDaysBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(25);
        when(vacationRepository.sumReservedDays(5L, 2026)).thenReturn(0);
        when(vacationRepository.save(any(Vacation.class))).thenAnswer(inv -> inv.getArgument(0));

        // 25 dias úteis passariam do direito antigo (22) e cabem no configurado (30).
        var dto = service.submitVacation(new CreateVacationRequest(
                5L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 10), 2026, null));

        assertEquals(25, dto.totalDays());
    }

    @Test
    void vacationBeyondTheConfiguredEntitlement_saysTheRealNumber() { // RHC-71
        Employee emp = employee(5L, "EMP-5", "Maria");
        when(employeeRepository.findByIdAndCompanyId(5L, 7L)).thenReturn(Optional.of(emp));
        when(hrPolicyService.annualVacationDays(any(Integer.class), any(LocalDate.class)))
                .thenReturn(Optional.of(30));
        when(timeSheetService.workingDaysBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(31);

        var ex = assertThrows(BusinessRuleException.class, () -> service.submitVacation(
                new CreateVacationRequest(5L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 20), 2026, null)));

        assertEquals(true, ex.getMessage().contains("direito anual 30"));
        assertEquals(true, ex.getMessage().contains("útil(eis)"));
    }

    @Test
    void payslipInAClosedPayrollMonth_isRefusedByName() { // RHC-74
        // Um mês já pago, entregue ao Estado e contabilizado continuava a aceitar recibos novos — e
        // cada recibo novo desalinhava a retenção já declarada.
        org.mockito.Mockito.doThrow(new BusinessRuleException("A folha de 8/2026 está fechada."))
                .when(payrollPeriodService).ensureOpen(2026, 8);

        var ex = assertThrows(BusinessRuleException.class, () -> service.createPayslip(
                new CreatePayslipRequest(5L, 2026, 8, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null)));

        assertEquals(true, ex.getMessage().contains("8/2026"));
        verify(payslipRepository, never()).save(any());
    }

    @Test
    void absenceDeduction_usesTheDeclaredUnpaidTypes() { // RHC-73
        // A regra vivia num literal da consulta ('UNJUSTIFIED'). Baixa médica e maternidade estavam
        // pagas por acidente, e um tipo novo passaria a ser pago sem ninguém ter decidido isso.
        Employee emp = employee(5L, "EMP-5", "Maria");
        emp.setBaseSalary(new BigDecimal("30000"));
        stubPayslipCreation(emp);
        when(salaryHistoryService.salaryOn(eq(5L), any())).thenReturn(Optional.of(new BigDecimal("30000")));

        service.createPayslip(new CreatePayslipRequest(5L, 2026, 8, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null));

        verify(absenceRepository).findUnpaidOverlapping(eq(5L), any(), any(),
                eq(mz.multicore.erp.modules.hr.model.AbsencePayRule.unpaidTypes()));
    }

    private static Payslip approvedPayslipToPay() {
        Payslip p = payslip(employee(5L, "EMP-5", "Maria"));
        p.setStatus("APPROVED");
        p.setBaseSalary(new BigDecimal("30000"));
        p.setAllowances(new BigDecimal("1000"));
        p.setOvertime(BigDecimal.ZERO);
        p.setIrpsDeduction(new BigDecimal("3000"));
        p.setInssDeduction(new BigDecimal("900"));
        p.setEmployerInss(new BigDecimal("900"));
        p.setOtherDeductions(new BigDecimal("1000"));
        p.setAbsenceDeduction(BigDecimal.ZERO);
        p.setNetPay(new BigDecimal("26100"));
        return p;
    }

    /** Stubs mínimos para o {@code createPayslip} correr até ao fim dentro da folha mensal. */
    private void stubPayslipCreation(Employee employee) {
        stubPayslipCreation(employee, 8);
    }

    private void stubPayslipCreation(Employee employee, int month) {
        when(employeeRepository.findByIdAndCompanyId(employee.getId(), 7L)).thenReturn(Optional.of(employee));
        when(payslipRepository.findByEmployeeIdAndYearAndMonth(employee.getId(), 2026, month))
                .thenReturn(Optional.empty());
        when(documentNumberService.next(any())).thenReturn("REC-2026/1");
        when(absenceRepository.findUnpaidOverlapping(eq(employee.getId()), any(), any(), any()))
                .thenReturn(List.of());
        when(payrollTaxService.calculate(any(), any(), any(), eq(2026), eq(month)))
                .thenReturn(new PayrollCalculationDTO(
                        BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "cfg", "lei"));
        when(payslipRepository.save(any(Payslip.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Payslip payslip(Employee owner) {
        Payslip p = new Payslip();
        p.setEmployee(owner);
        p.setPayslipNumber("REC-2026/" + owner.getId());
        p.setYear(2026);
        p.setMonth(6);
        return p;
    }

    private static UpsertEmployeeRequest requestWithUser(String number, String email, String username) {
        UpsertEmployeeRequest base = request(number, email);
        return new UpsertEmployeeRequest(
                base.employeeNumber(), base.name(), base.email(), base.phone(), base.photo(), base.taxId(),
                base.inssNumber(), base.dependentsCount(), base.department(), base.role(),
                base.baseSalary(), base.hireDate(), base.contractEndDate(), username,
                base.bankName(), base.bankAccount());
    }

    private static UpsertEmployeeRequest request(String number, String email) {
        return new UpsertEmployeeRequest(
                number, "Novo Colaborador", email, null, null, null, null, 0,
                "Operações", "EMPLOYEE", new BigDecimal("25000"),
                LocalDate.of(2026, 1, 1), null, null, null, null
        );
    }

    // ─── §B8.3: a folha passa a perguntar QUANDO, não só se a pessoa está activa hoje ───────────

    @Test
    void createPayslip_afterContractEnded_isRefusedNamingTheDate() {
        Employee gone = employee(30L, "EMP-30", "Contrato Findo");
        gone.setHireDate(LocalDate.of(2024, 1, 10));
        // Contrato a prazo terminou em Maio; ninguém correu a cessação, pelo que continua ACTIVE.
        gone.setContractEndDate(LocalDate.of(2026, 5, 31));
        when(employeeRepository.findByIdAndCompanyId(30L, 7L)).thenReturn(Optional.of(gone));

        var ex = assertThrows(BusinessRuleException.class, () -> service.createPayslip(
                new CreatePayslipRequest(30L, 2026, 6, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null)));
        assertEquals(true, ex.getMessage().contains("terminou a 31/05/2026"));
        // Nada foi gravado: a recusa é antes de existir recibo.
        verify(payslipRepository, never()).save(any(Payslip.class));
    }

    @Test
    void createPayslip_monthOfTermination_isStillAllowed() {
        Employee leaving = employee(31L, "EMP-31", "Saiu A Vinte");
        leaving.setHireDate(LocalDate.of(2024, 1, 10));
        leaving.setContractEndDate(LocalDate.of(2026, 6, 20)); // saiu a meio de Junho
        when(employeeRepository.findByIdAndCompanyId(31L, 7L)).thenReturn(Optional.of(leaving));
        when(payslipRepository.findByEmployeeIdAndYearAndMonth(31L, 2026, 6)).thenReturn(Optional.empty());
        when(documentNumberService.next(any())).thenReturn("REC-2026/9");
        when(payrollTaxService.calculate(any(), any(), any(), eq(2026), eq(6)))
                .thenReturn(new PayrollCalculationDTO(
                        BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, "cfg", "lei"));
        when(payslipRepository.save(any(Payslip.class))).thenAnswer(inv -> inv.getArgument(0));

        // Trabalhou 20 dias de Junho: o recibo desse mês é legítimo. A guarda é contra a DATA,
        // não contra o facto de a pessoa ter saído — senão o último recibo era sempre impossível.
        var dto = service.createPayslip(new CreatePayslipRequest(
                31L, 2026, 6, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null));

        assertEquals("REC-2026/9", dto.payslipNumber());
    }

    @Test
    void createPayslip_beforeHireDate_isRefused() {
        Employee recent = employee(32L, "EMP-32", "Admitido Agora");
        recent.setHireDate(LocalDate.of(2026, 6, 15));
        when(employeeRepository.findByIdAndCompanyId(32L, 7L)).thenReturn(Optional.of(recent));

        var ex = assertThrows(BusinessRuleException.class, () -> service.createPayslip(
                new CreatePayslipRequest(32L, 2026, 3, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null)));
        assertEquals(true, ex.getMessage().contains("admitido a 15/06/2026"));
        verify(payslipRepository, never()).save(any(Payslip.class));
    }

    @Test
    void wasEmployedDuring_withoutDates_imposesNoRestriction() {
        // Quem não tem admissão nem fim registados continua exactamente como antes: esta guarda
        // não pode transformar fichas incompletas — o caso normal numa loja — em folha bloqueada.
        Employee bare = employee(33L, "EMP-33", "Sem Datas");

        assertEquals(true, bare.wasEmployedDuring(2026, 6));
        assertEquals(true, bare.wasEmployedDuring(1999, 1));
    }

    private static Employee employee(Long id, String number, String name) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setEmployeeNumber(number);
        employee.setName(name);
        employee.setEmail(name.toLowerCase() + "@empresa.test");
        employee.setDepartment("RH");
        employee.setRole("EMPLOYEE");
        employee.setBaseSalary(BigDecimal.TEN);
        employee.setStatus("ACTIVE");
        return employee;
    }
}
