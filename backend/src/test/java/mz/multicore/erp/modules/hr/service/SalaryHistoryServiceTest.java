package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.hr.dto.CreateSalaryChangeRequest;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.SalaryChange;
import mz.multicore.erp.modules.hr.model.SalaryChangeReason;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.SalaryChangeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * B4 do harness do RH (RHC-45..47). O caso que carrega a classe é o {@code salaryOn}: é dele que
 * depende o recibo de Março pagar ao valor de Março.
 */
class SalaryHistoryServiceTest {

    private static final Long COMPANY = 7L;

    private SalaryChangeRepository salaryChangeRepository;
    private EmployeeRepository employeeRepository;
    private CompanyRepository companyRepository;
    private AuditLogService auditLogService;
    private SalaryHistoryService service;

    @BeforeEach
    void setUp() {
        salaryChangeRepository = mock(SalaryChangeRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        companyRepository = mock(CompanyRepository.class);
        auditLogService = mock(AuditLogService.class);
        service = new SalaryHistoryService(salaryChangeRepository, employeeRepository,
                companyRepository, auditLogService);

        Company company = new Company();
        company.setId(COMPANY);
        when(companyRepository.findById(COMPANY)).thenReturn(Optional.of(company));
        when(employeeRepository.findByIdAndCompanyId(5L, COMPANY)).thenReturn(Optional.of(employee()));
        when(salaryChangeRepository.save(any(SalaryChange.class))).thenAnswer(inv -> inv.getArgument(0));
        CurrentUserContext.setCurrentCompanyId(COMPANY);
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void registerChange_preservesThePreviousSalary() { // RHC-45
        // O defeito que este bloco fecha: o valor anterior não ficava em lado nenhum.
        var dto = service.registerChange(request(new BigDecimal("35000"), LocalDate.of(2026, 6, 1)));

        assertEquals(0, new BigDecimal("30000").compareTo(dto.previousSalary()));
        assertEquals(0, new BigDecimal("35000").compareTo(dto.newSalary()));
        assertEquals(0, new BigDecimal("5000").compareTo(dto.difference()));
        assertEquals("gestor", dto.approvedBy());
        verify(auditLogService).logCurrent(eq("SALARY_CHANGE"), any());
    }

    @Test
    void salaryOn_returnsTheValueInForceAtThatDate() { // RHC-46 — o caso que dói
        // Aumento em Junho: perguntar por Março tem de continuar a dar o valor de Março.
        when(salaryChangeRepository.findEffectiveOn(eq(5L), eq(COMPANY), eq(LocalDate.of(2026, 3, 31))))
                .thenReturn(List.of(change(new BigDecimal("30000"), LocalDate.of(2026, 1, 1))));
        when(salaryChangeRepository.findEffectiveOn(eq(5L), eq(COMPANY), eq(LocalDate.of(2026, 9, 30))))
                .thenReturn(List.of(
                        change(new BigDecimal("35000"), LocalDate.of(2026, 6, 1)),
                        change(new BigDecimal("30000"), LocalDate.of(2026, 1, 1))));

        assertEquals(0, new BigDecimal("30000").compareTo(
                service.salaryOn(5L, LocalDate.of(2026, 3, 31)).orElseThrow()));
        assertEquals(0, new BigDecimal("35000").compareTo(
                service.salaryOn(5L, LocalDate.of(2026, 9, 30)).orElseThrow()));
    }

    @Test
    void salaryOn_withoutAnyHistory_isEmpty() {
        // Colaborador anterior a este bloco: quem chama fica com o valor da ficha, como sempre.
        when(salaryChangeRepository.findEffectiveOn(eq(5L), eq(COMPANY), any())).thenReturn(List.of());

        assertTrue(service.salaryOn(5L, LocalDate.of(2026, 3, 31)).isEmpty());
    }

    @Test
    void futureChange_doesNotTouchTheEmployeeCardYet() {
        // Uma alteração com data futura é um compromisso, não um facto: a ficha só muda quando lá
        // chegar, e é a data de efeito que manda no recibo até lá.
        service.registerChange(request(new BigDecimal("40000"), LocalDate.now().plusMonths(2)));

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void changeEffectiveToday_updatesTheEmployeeCard() {
        service.registerChange(request(new BigDecimal("40000"), LocalDate.now()));

        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void sameSalary_isNotAChange() {
        var ex = assertThrows(BusinessRuleException.class,
                () -> service.registerChange(request(new BigDecimal("30000"), LocalDate.of(2026, 6, 1))));
        assertTrue(ex.getMessage().contains("não há alteração a registar"));
    }

    @Test
    void twoChangesOnTheSameEffectiveDate_areBlocked() {
        // Duas no mesmo dia tornariam ambígua a única pergunta que esta tabela existe para responder.
        when(salaryChangeRepository.existsByCompanyIdAndEmployeeIdAndEffectiveDate(
                COMPANY, 5L, LocalDate.of(2026, 6, 1))).thenReturn(true);

        var ex = assertThrows(BusinessRuleException.class,
                () -> service.registerChange(request(new BigDecimal("35000"), LocalDate.of(2026, 6, 1))));
        assertTrue(ex.getMessage().contains("já tem uma alteração salarial"));
    }

    @Test
    void employeeRole_cannotChangeSalaries() {
        CurrentUserContext.setCurrentUser("operador", "EMPLOYEE");

        assertThrows(BusinessRuleException.class,
                () -> service.registerChange(request(new BigDecimal("35000"), LocalDate.of(2026, 6, 1))));
        verify(salaryChangeRepository, never()).save(any());
    }

    @Test
    void promotionAlsoCarriesTheNewRole() {
        var dto = service.registerChange(new CreateSalaryChangeRequest(5L, new BigDecimal("38000"),
                LocalDate.now(), "PROMOCAO", "Chefe de Loja", "Operações", "Promoção anual"));

        assertEquals("PROMOCAO", dto.reason());
        assertEquals("Chefe de Loja", dto.jobTitle());
        assertEquals("Operações", dto.department());
    }

    private CreateSalaryChangeRequest request(BigDecimal newSalary, LocalDate effectiveDate) {
        return new CreateSalaryChangeRequest(5L, newSalary, effectiveDate, "AUMENTO", null, null, null);
    }

    private SalaryChange change(BigDecimal salary, LocalDate effectiveDate) {
        SalaryChange change = new SalaryChange();
        change.setEmployee(employee());
        change.setNewSalary(salary);
        change.setEffectiveDate(effectiveDate);
        change.setReason(SalaryChangeReason.AUMENTO);
        change.setApprovedBy("gestor");
        return change;
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
