package com.phcpro.modules.hr.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.hr.dto.PayrollCalculationDTO;
import com.phcpro.modules.hr.dto.PayrollFiscalSummaryDTO;
import com.phcpro.modules.hr.model.Employee;
import com.phcpro.modules.hr.model.PayrollIrpsBracket;
import com.phcpro.modules.hr.model.PayrollTaxConfig;
import com.phcpro.modules.hr.model.Payslip;
import com.phcpro.modules.hr.repository.PayrollTaxConfigRepository;
import com.phcpro.modules.hr.repository.PayslipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class PayrollTaxService {
    private final PayrollTaxConfigRepository configRepository;
    private final PayslipRepository payslipRepository;

    public PayrollTaxService(PayrollTaxConfigRepository configRepository, PayslipRepository payslipRepository) {
        this.configRepository = configRepository;
        this.payslipRepository = payslipRepository;
    }

    @Transactional(readOnly = true)
    public PayrollCalculationDTO calculate(Employee employee, BigDecimal allowances, BigDecimal overtime, int year, int month) {
        LocalDate period = LocalDate.of(year, month, 1);
        PayrollTaxConfig config = applicableConfig(employee.getCompany().getId(), period);
        BigDecimal gross = money(employee.getBaseSalary().add(orZero(allowances)).add(orZero(overtime)));
        BigDecimal taxable = gross;
        PayrollIrpsBracket bracket = config.getBrackets().stream()
                .filter(b -> taxable.compareTo(b.getLowerBound()) >= 0)
                .filter(b -> b.getUpperBound() == null || taxable.compareTo(b.getUpperBound()) <= 0)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("A configuração fiscal não possui escalão IRPS para este rendimento."));

        BigDecimal irps = taxable.multiply(bracket.getRate())
                .subtract(bracket.getFixedDeduction())
                .subtract(bracket.getDependentDeduction().multiply(BigDecimal.valueOf(employee.getDependentsCount())));
        irps = money(irps.max(BigDecimal.ZERO));
        BigDecimal employeeInss = money(gross.multiply(config.getEmployeeInssRate()));
        BigDecimal employerInss = money(gross.multiply(config.getEmployerInssRate()));
        BigDecimal net = money(gross.subtract(irps).subtract(employeeInss));

        return new PayrollCalculationDTO(
                gross, taxable, irps, employeeInss, employerInss, net,
                bracket.getRate(), config.getEmployeeInssRate(), config.getEmployerInssRate(),
                config.getName(), config.getLegalBasis()
        );
    }

    @Transactional(readOnly = true)
    public PayrollFiscalSummaryDTO fiscalSummary(int year, int month) {
        List<Payslip> payslips = payslipRepository.findByCompanyIdAndYearAndMonth(
                CurrentUserContext.getCurrentCompanyId(), year, month);
        var lines = payslips.stream().filter(p -> !"CANCELLED".equals(p.getStatus())).map(p ->
                new PayrollFiscalSummaryDTO.PayrollFiscalLineDTO(
                        p.getEmployee().getEmployeeNumber(), p.getEmployee().getName(),
                        p.getEmployee().getTaxId(), p.getEmployee().getInssNumber(),
                        gross(p), p.getTaxableIncome(), p.getIrpsDeduction(),
                        p.getInssDeduction(), p.getEmployerInss()
                )).toList();
        BigDecimal gross = lines.stream().map(PayrollFiscalSummaryDTO.PayrollFiscalLineDTO::grossPay).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taxable = lines.stream().map(PayrollFiscalSummaryDTO.PayrollFiscalLineDTO::taxableIncome).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal irps = lines.stream().map(PayrollFiscalSummaryDTO.PayrollFiscalLineDTO::irps).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal employeeInss = lines.stream().map(PayrollFiscalSummaryDTO.PayrollFiscalLineDTO::employeeInss).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal employerInss = lines.stream().map(PayrollFiscalSummaryDTO.PayrollFiscalLineDTO::employerInss).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PayrollFiscalSummaryDTO(year, month, gross, taxable, irps, employeeInss, employerInss,
                employeeInss.add(employerInss), lines);
    }

    private PayrollTaxConfig applicableConfig(Long companyId, LocalDate date) {
        return configRepository.findApplicable(companyId, date).stream().findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "Não existe configuração fiscal salarial ativa para " + date.getMonthValue() + "/" + date.getYear() + "."));
    }

    private BigDecimal gross(Payslip p) {
        return p.getBaseSalary().add(p.getAllowances()).add(p.getOvertime());
    }

    private BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
