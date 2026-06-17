package com.phcpro.modules.hr.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.approvals.service.ApprovalService;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.company.repository.CompanyRepository;
import com.phcpro.modules.hr.dto.UpsertEmployeeRequest;
import com.phcpro.modules.hr.model.Employee;
import com.phcpro.modules.hr.repository.AbsenceRepository;
import com.phcpro.modules.hr.repository.EmployeeRepository;
import com.phcpro.modules.hr.repository.ExpenseClaimRepository;
import com.phcpro.modules.hr.repository.PayslipRepository;
import com.phcpro.modules.hr.repository.VacationRepository;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HRServiceTest {

    private EmployeeRepository employeeRepository;
    private CompanyRepository companyRepository;
    private HRService service;

    @BeforeEach
    void setUp() {
        employeeRepository = mock(EmployeeRepository.class);
        companyRepository = mock(CompanyRepository.class);
        service = new HRService(
                employeeRepository,
                mock(ExpenseClaimRepository.class),
                mock(PayslipRepository.class),
                mock(AbsenceRepository.class),
                mock(VacationRepository.class),
                companyRepository,
                mock(PayrollTaxService.class),
                mock(ApprovalService.class)
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
