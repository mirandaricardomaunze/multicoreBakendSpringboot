package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.approvals.service.ApprovalService;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.hr.dto.CreatePayslipRequest;
import mz.multicore.erp.modules.hr.dto.CreateVacationRequest;
import mz.multicore.erp.modules.hr.dto.PayrollCalculationDTO;
import mz.multicore.erp.modules.hr.dto.UpsertEmployeeRequest;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.hr.model.Absence;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.Payslip;
import mz.multicore.erp.modules.hr.repository.AbsenceRepository;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.ExpenseClaimRepository;
import mz.multicore.erp.modules.hr.repository.PayslipRepository;
import mz.multicore.erp.modules.hr.repository.VacationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
        service = new HRService(
                employeeRepository,
                mock(ExpenseClaimRepository.class),
                payslipRepository,
                absenceRepository,
                vacationRepository,
                companyRepository,
                payrollTaxService,
                mock(ApprovalService.class),
                documentNumberService,
                auditLogService,
                mock(FinanceService.class)
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
        when(absenceRepository.findUnjustifiedOverlapping(eq(8L), any(), any())).thenReturn(List.of(ab));
        // Impostos a zero para isolar o desconto por faltas.
        when(payrollTaxService.calculate(any(), any(), any(), eq(2026), eq(6)))
                .thenReturn(new PayrollCalculationDTO(
                        new BigDecimal("30000"), new BigDecimal("30000"),
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("30000"),
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "cfg", "lei"));
        when(payslipRepository.save(any(Payslip.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = service.createPayslip(new CreatePayslipRequest(
                8L, 2026, 6, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null));

        assertEquals(0, new BigDecimal("3000.00").compareTo(dto.absenceDeduction()));
        assertEquals(0, new BigDecimal("27000.00").compareTo(dto.netPay())); // 30000 − 3000
    }

    private static UpsertEmployeeRequest request(String number, String email) {
        return new UpsertEmployeeRequest(
                number, "Novo Colaborador", email, null, null, null, 0,
                "Operações", "EMPLOYEE", new BigDecimal("25000"),
                LocalDate.of(2026, 1, 1), null
        );
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
