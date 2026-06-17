package com.phcpro.modules.hr.service;

import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.hr.model.Employee;
import com.phcpro.modules.hr.model.PayrollIrpsBracket;
import com.phcpro.modules.hr.model.PayrollTaxConfig;
import com.phcpro.modules.hr.repository.PayrollTaxConfigRepository;
import com.phcpro.modules.hr.repository.PayslipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PayrollTaxServiceTest {
    private PayrollTaxConfigRepository configRepository;
    private PayrollTaxService service;
    private Employee employee;

    @BeforeEach
    void setUp() {
        configRepository = mock(PayrollTaxConfigRepository.class);
        service = new PayrollTaxService(configRepository, mock(PayslipRepository.class));
        Company company = new Company();
        company.setId(1L);
        employee = new Employee();
        employee.setCompany(company);
        employee.setBaseSalary(new BigDecimal("20000"));
        employee.setDependentsCount(0);
        when(configRepository.findApplicable(1L, LocalDate.of(2026, 6, 1))).thenReturn(List.of(config()));
    }

    @Test
    void calculatesProgressiveIrpsAndEmployeeEmployerInss() {
        var result = service.calculate(employee, BigDecimal.ZERO, BigDecimal.ZERO, 2026, 6);

        assertEquals(new BigDecimal("3125.00"), result.irps());
        assertEquals(new BigDecimal("600.00"), result.employeeInss());
        assertEquals(new BigDecimal("800.00"), result.employerInss());
        assertEquals(new BigDecimal("16275.00"), result.netPay());
    }

    @Test
    void appliesDependentDeductionWithoutNegativeTax() {
        employee.setBaseSalary(new BigDecimal("1000"));
        employee.setDependentsCount(2);
        PayrollTaxConfig config = config();
        config.getBrackets().get(0).setDependentDeduction(new BigDecimal("100"));
        when(configRepository.findApplicable(1L, LocalDate.of(2026, 6, 1))).thenReturn(List.of(config));

        var result = service.calculate(employee, BigDecimal.ZERO, BigDecimal.ZERO, 2026, 6);

        assertEquals(new BigDecimal("0.00"), result.irps());
        assertEquals(new BigDecimal("30.00"), result.employeeInss());
    }

    private PayrollTaxConfig config() {
        PayrollTaxConfig config = new PayrollTaxConfig();
        config.setName("Teste");
        config.setEmployeeInssRate(new BigDecimal("0.03"));
        config.setEmployerInssRate(new BigDecimal("0.04"));
        config.setLegalBasis("Teste");
        add(config, "0", "3500", "0.10", "0");
        add(config, "3500.01", "14000", "0.15", "175");
        add(config, "14000.01", "42000", "0.20", "875");
        return config;
    }

    private void add(PayrollTaxConfig config, String lower, String upper, String rate, String deduction) {
        PayrollIrpsBracket bracket = new PayrollIrpsBracket();
        bracket.setConfig(config);
        bracket.setLowerBound(new BigDecimal(lower));
        bracket.setUpperBound(new BigDecimal(upper));
        bracket.setRate(new BigDecimal(rate));
        bracket.setFixedDeduction(new BigDecimal(deduction));
        bracket.setDependentDeduction(BigDecimal.ZERO);
        config.getBrackets().add(bracket);
    }
}
