package com.phcpro.modules.hr.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.audit.service.AuditLogService;
import com.phcpro.modules.financeira.service.FinanceService;
import com.phcpro.modules.hr.dto.ThirteenthMonthDTO;
import com.phcpro.modules.hr.dto.VacationAllowanceDTO;
import com.phcpro.modules.hr.model.Employee;
import com.phcpro.modules.hr.model.PayrollBonus;
import com.phcpro.modules.hr.model.Vacation;
import com.phcpro.modules.hr.repository.EmployeeRepository;
import com.phcpro.modules.hr.repository.PayrollBonusRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PayrollBonusServiceTest {

    private EmployeeRepository employeeRepository;
    private VacationRepository vacationRepository;
    private PayrollBonusRepository bonusRepository;
    private FinanceService financeService;
    private PayrollBonusService service;

    @BeforeEach
    void setUp() {
        employeeRepository = mock(EmployeeRepository.class);
        vacationRepository = mock(VacationRepository.class);
        bonusRepository = mock(PayrollBonusRepository.class);
        financeService = mock(FinanceService.class);
        service = new PayrollBonusService(employeeRepository, vacationRepository,
                bonusRepository, mock(AuditLogService.class), financeService);
        CurrentUserContext.setCurrentCompanyId(7L);
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void thirteenthMonth_isProportionalToTenure() {
        Employee full = employee(1L, "EMP-1", "Inteiro", new BigDecimal("24000"),
                LocalDate.of(2024, 3, 1), null); // ano inteiro → 12 meses → 24000
        Employee half = employee(2L, "EMP-2", "Meio", new BigDecimal("12000"),
                LocalDate.of(2026, 7, 1), null); // Jul..Dez → 6 meses → 6000
        when(employeeRepository.findByCompanyIdOrderByName(7L)).thenReturn(List.of(full, half));

        ThirteenthMonthDTO dto = service.thirteenthMonth(2026);

        assertEquals(2, dto.lines().size());
        assertEquals(0, new BigDecimal("24000.00").compareTo(dto.lines().get(0).amount()));
        assertEquals(6, dto.lines().get(1).monthsWorked());
        assertEquals(0, new BigDecimal("6000.00").compareTo(dto.lines().get(1).amount()));
        assertEquals(0, new BigDecimal("30000.00").compareTo(dto.total()));
    }

    @Test
    void thirteenthMonth_excludesEmployeeHiredAfterYear() {
        Employee future = employee(3L, "EMP-3", "Futuro", new BigDecimal("10000"),
                LocalDate.of(2027, 1, 1), null);
        when(employeeRepository.findByCompanyIdOrderByName(7L)).thenReturn(List.of(future));

        ThirteenthMonthDTO dto = service.thirteenthMonth(2026);

        assertEquals(0, dto.lines().size());
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.total()));
    }

    @Test
    void vacationAllowance_approved_usesDailyRate() {
        Employee e = employee(5L, "EMP-5", "Ferias", new BigDecimal("30000"), LocalDate.of(2024, 1, 1), null);
        Vacation v = new Vacation();
        v.setId(50L);
        v.setEmployee(e);
        v.setTotalDays(10);
        v.setStatus("APPROVED");
        when(vacationRepository.findByIdAndEmployeeCompanyId(50L, 7L)).thenReturn(Optional.of(v));

        VacationAllowanceDTO dto = service.vacationAllowance(50L);

        assertEquals(0, new BigDecimal("1000.00").compareTo(dto.dailyRate())); // 30000/30
        assertEquals(0, new BigDecimal("10000.00").compareTo(dto.amount()));   // 10 dias
    }

    @Test
    void vacationAllowance_notApproved_isBlocked() {
        Employee e = employee(6L, "EMP-6", "Pendente", new BigDecimal("30000"), LocalDate.of(2024, 1, 1), null);
        Vacation v = new Vacation();
        v.setId(60L);
        v.setEmployee(e);
        v.setTotalDays(5);
        v.setStatus("PENDING");
        when(vacationRepository.findByIdAndEmployeeCompanyId(60L, 7L)).thenReturn(Optional.of(v));

        assertThrows(BusinessRuleException.class, () -> service.vacationAllowance(60L));
    }

    @Test
    void payThirteenthMonth_isIdempotent_skipsAlreadyPaid() {
        Employee a = employee(1L, "EMP-1", "Pago", new BigDecimal("24000"), LocalDate.of(2024, 1, 1), null);
        Employee b = employee(2L, "EMP-2", "Novo", new BigDecimal("12000"), LocalDate.of(2024, 1, 1), null);
        when(employeeRepository.findByCompanyIdOrderByName(7L)).thenReturn(List.of(a, b));
        when(bonusRepository.existsByEmployeeIdAndBonusTypeAndYear(1L, "THIRTEENTH_MONTH", 2026)).thenReturn(true);
        when(bonusRepository.existsByEmployeeIdAndBonusTypeAndYear(2L, "THIRTEENTH_MONTH", 2026)).thenReturn(false);

        ThirteenthMonthDTO paid = service.payThirteenthMonth(2026);

        // Só o colaborador ainda não pago entra; uma só saída de tesouraria e um só registo.
        assertEquals(1, paid.lines().size());
        assertEquals("EMP-2", paid.lines().get(0).employeeNumber());
        verify(bonusRepository, times(1)).save(any(PayrollBonus.class));
        verify(financeService, times(1)).registerAutoPayout(any(), any());
    }

    @Test
    void payVacationAllowance_alreadyPaid_isBlocked() {
        Employee e = employee(5L, "EMP-5", "Ferias", new BigDecimal("30000"), LocalDate.of(2024, 1, 1), null);
        Vacation v = new Vacation();
        v.setId(50L);
        v.setEmployee(e);
        v.setTotalDays(10);
        v.setStatus("APPROVED");
        when(vacationRepository.findByIdAndEmployeeCompanyId(50L, 7L)).thenReturn(Optional.of(v));
        when(bonusRepository.existsByBonusTypeAndReferenceId(eq("VACATION_ALLOWANCE"), eq(50L))).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> service.payVacationAllowance(50L));
        verify(financeService, never()).registerAutoPayout(any(), any());
    }

    private static Employee employee(Long id, String number, String name, BigDecimal base,
                                     LocalDate hire, LocalDate contractEnd) {
        Employee e = new Employee();
        e.setId(id);
        e.setEmployeeNumber(number);
        e.setName(name);
        e.setBaseSalary(base);
        e.setHireDate(hire);
        e.setContractEndDate(contractEnd);
        e.setStatus("ACTIVE");
        return e;
    }
}
