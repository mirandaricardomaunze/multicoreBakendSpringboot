package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.hr.dto.CreatePayslipRequest;
import mz.multicore.erp.modules.hr.dto.PayrollRunDTO;
import mz.multicore.erp.modules.hr.dto.PayslipDTO;
import mz.multicore.erp.modules.hr.model.AbsencePayRule;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.Payslip;
import mz.multicore.erp.modules.hr.repository.AbsenceRepository;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.PayslipRepository;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import mz.multicore.erp.modules.performance.model.BonusStatus;
import mz.multicore.erp.modules.performance.model.SalesGoalBonus;
import mz.multicore.erp.modules.performance.repository.SalesGoalBonusRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
public class HrPayslipIssuanceService {

    private static final DateTimeFormatter DATE_PT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmployeeRepository employeeRepository;
    private final PayslipRepository payslipRepository;
    private final AbsenceRepository absenceRepository;
    private final HrAccessGuard guard;
    private final PayrollTaxService payrollTaxService;
    private final DocumentNumberService documentNumberService;
    private final AuditLogService auditLogService;
    private final EmploymentContractService contractService;
    private final TimeSheetService timeSheetService;
    private final OvertimeValuationService overtimeValuationService;
    private final SalaryHistoryService salaryHistoryService;
    private final PayrollDeductionService payrollDeductionService;
    private final PayrollPeriodService payrollPeriodService;
    private final SalesGoalBonusRepository salesGoalBonusRepository;

    public HrPayslipIssuanceService(
            EmployeeRepository employeeRepository, PayslipRepository payslipRepository,
            AbsenceRepository absenceRepository, HrAccessGuard guard, PayrollTaxService payrollTaxService,
            DocumentNumberService documentNumberService, AuditLogService auditLogService,
            EmploymentContractService contractService, @Lazy TimeSheetService timeSheetService,
            @Lazy OvertimeValuationService overtimeValuationService, SalaryHistoryService salaryHistoryService,
            @Lazy PayrollDeductionService payrollDeductionService, PayrollPeriodService payrollPeriodService,
            @Lazy SalesGoalBonusRepository salesGoalBonusRepository) {
        this.employeeRepository = employeeRepository;
        this.payslipRepository = payslipRepository;
        this.absenceRepository = absenceRepository;
        this.guard = guard;
        this.payrollTaxService = payrollTaxService;
        this.documentNumberService = documentNumberService;
        this.auditLogService = auditLogService;
        this.contractService = contractService;
        this.timeSheetService = timeSheetService;
        this.overtimeValuationService = overtimeValuationService;
        this.salaryHistoryService = salaryHistoryService;
        this.payrollDeductionService = payrollDeductionService;
        this.payrollPeriodService = payrollPeriodService;
        this.salesGoalBonusRepository = salesGoalBonusRepository;
    }

    public PayslipDTO createPayslip(CreatePayslipRequest request) {
        guard.ensureHrManager();
        payrollPeriodService.ensureOpen(request.year(), request.month());
        Employee employee = guard.findActiveEmployee(request.employeeId());
        ensureEmploymentCovers(employee, request.year(), request.month());
        if (payslipRepository.findByEmployeeIdAndYearAndMonth(employee.getId(), request.year(), request.month()).isPresent()) {
            throw new BusinessRuleException("Já existe um recibo para este colaborador em " + request.month() + "/" + request.year() + ".");
        }
        Payslip p = new Payslip();
        p.setEmployee(employee);
        p.setCompany(employee.getCompany());
        p.setYear(request.year());
        p.setMonth(request.month());
        p.setBaseSalary(salaryForPeriod(employee, request.year(), request.month()));
        p.setAllowances(orZero(request.allowances()));
        p.setOvertime(resolveOvertime(employee, request, p));
        var tax = payrollTaxService.calculate(employee, p.getAllowances(), p.getOvertime(), request.year(), request.month());
        p.setTaxableIncome(tax.taxableIncome());
        p.setIrpsDeduction(tax.irps());
        p.setInssDeduction(tax.employeeInss());
        p.setEmployerInss(tax.employerInss());
        p.setIrpsRate(tax.irpsRate());
        p.setEmployeeInssRate(tax.employeeInssRate());
        p.setEmployerInssRate(tax.employerInssRate());
        p.setTaxConfigName(tax.configName());
        p.setTaxLegalBasis(tax.legalBasis());
        p.setOtherDeductions(orZero(request.otherDeductions()));
        p.setAbsenceDeduction(calculateAbsenceDeduction(employee, request.year(), request.month()));

        List<SalesGoalBonus> approvedBonuses = salesGoalBonusRepository != null
                ? salesGoalBonusRepository.findByCompanyIdAndEmployeeIdAndStatus(employee.getCompany().getId(), employee.getId(), BonusStatus.APPROVED)
                        .stream().filter(b -> b.getPayslip() == null).toList()
                : List.of();
        p.setSalesBonus(approvedBonuses.stream().map(SalesGoalBonus::getApprovedAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        p.setNetPay(calculateNet(p));
        p.setStatus("DRAFT");
        p.setNotes(request.notes());
        p.setPayslipNumber(documentNumberService.next(DocumentSeries.PAYSLIP));

        Payslip saved = payslipRepository.save(p);
        if (salesGoalBonusRepository != null) {
            for (SalesGoalBonus bonus : approvedBonuses) {
                bonus.setPayslip(saved);
                bonus.setStatus(BonusStatus.PAID);
                salesGoalBonusRepository.save(bonus);
            }
        }
        saved = applyCommittedDeductions(saved);
        auditLogService.logCurrent("PAYSLIP_ISSUE", String.format(
                "Recibo %s emitido para %s (%d/%d), líquido %s",
                saved.getPayslipNumber(), employee.getName(), request.month(), request.year(), saved.getNetPay()));
        return payslipToDTO(saved);
    }

    public PayrollRunDTO processMonthlyPayroll(int year, int month) {
        guard.ensureHrManager();
        ensureTimeSheetClosed(year, month);
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

        List<PayslipDTO> generated = new ArrayList<>();
        List<PayrollRunDTO.PayrollSkipDTO> skipped = new ArrayList<>();

        for (Employee employee : employeeRepository.findByCompanyIdOrderByName(guard.currentCompanyId())) {
            if (!"ACTIVE".equals(employee.getStatus())) {
                continue;
            }
            if (payslipRepository.findByEmployeeIdAndYearAndMonth(employee.getId(), year, month).isPresent()) {
                continue;
            }
            if (contractService.findContractInPeriod(employee.getId(), monthStart, monthEnd).isEmpty()) {
                skipped.add(new PayrollRunDTO.PayrollSkipDTO(employee.getId(), employee.getName(),
                        "Sem contrato vigente em " + month + "/" + year));
                continue;
            }
            generated.add(createPayslip(new CreatePayslipRequest(
                    employee.getId(), year, month, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    "Processamento mensal automático", null)));
        }

        auditLogService.logCurrent("PAYROLL_PROCESS", String.format(
                "Folha mensal %d/%d processada: %d recibo(s) gerado(s), %d colaborador(es) sem contrato vigente",
                month, year, generated.size(), skipped.size()));
        return new PayrollRunDTO(generated, skipped);
    }

    private Payslip applyCommittedDeductions(Payslip payslip) {
        BigDecimal gross = payslip.getBaseSalary().add(payslip.getAllowances()).add(payslip.getOvertime()).add(orZero(payslip.getSalesBonus()));
        BigDecimal room = gross
                .subtract(payslip.getIrpsDeduction())
                .subtract(payslip.getInssDeduction())
                .subtract(payslip.getAbsenceDeduction())
                .subtract(payslip.getOtherDeductions());
        BigDecimal committed = payrollDeductionService.applyTo(payslip, room.max(BigDecimal.ZERO));
        if (committed.signum() <= 0) {
            return payslip;
        }
        payslip.setOtherDeductions(payslip.getOtherDeductions().add(committed));
        payslip.setNetPay(calculateNet(payslip));
        return payslipRepository.save(payslip);
    }

    private BigDecimal salaryForPeriod(Employee employee, int year, int month) {
        LocalDate periodEnd = LocalDate.of(year, month, 1)
                .withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth());
        return salaryHistoryService.salaryOn(employee.getId(), periodEnd)
                .orElseGet(() -> employee.getBaseSalary() != null ? employee.getBaseSalary() : BigDecimal.ZERO);
    }

    private void ensureTimeSheetClosed(int year, int month) {
        if (timeSheetService.getEntries(year, month).isEmpty()) {
            return;
        }
        if (!timeSheetService.isPeriodClosed(year, month)) {
            throw new BusinessRuleException(String.format(
                    "A folha de ponto de %d/%d ainda está aberta. Feche-a antes de processar a folha "
                            + "salarial, para os salários não assentarem em horas que ainda podem mudar.",
                    month, year));
        }
    }

    private BigDecimal resolveOvertime(Employee employee, CreatePayslipRequest request, Payslip payslip) {
        BigDecimal manual = orZero(request.overtime());
        if (!timeSheetService.isPeriodClosed(request.year(), request.month())) {
            return manual;
        }
        var valuation = overtimeValuationService.valueFor(
                employee.getId(), request.year(), request.month(), payslip.getBaseSalary());
        if (valuation.isEmpty()) {
            return manual;
        }
        BigDecimal fromTimeSheet = valuation.get().amount();

        if (manual.signum() > 0 && manual.compareTo(fromTimeSheet) != 0) {
            if (request.overtimeOverrideReason() == null || request.overtimeOverrideReason().isBlank()) {
                throw new BusinessRuleException(String.format(
                        "A folha de ponto de %d/%d está fechada e apura %s de horas extra. "
                                + "Para usar outro valor indique a justificação da excepção.",
                        request.month(), request.year(), fromTimeSheet));
            }
            auditLogService.logCurrent("PAYSLIP_OVERTIME_OVERRIDE", String.format(
                    "Recibo de %s (%d/%d): horas extra manuais %s em vez das %s apuradas no ponto. Justificação: %s",
                    employee.getName(), request.month(), request.year(), manual, fromTimeSheet,
                    request.overtimeOverrideReason().trim()));
            return manual;
        }
        return fromTimeSheet;
    }

    private void ensureEmploymentCovers(Employee employee, int year, int month) {
        if (employee.wasEmployedDuring(year, month)) {
            return;
        }
        LocalDate monthStart = LocalDate.of(year, month, 1);
        if (employee.getHireDate() != null
                && employee.getHireDate().isAfter(monthStart.withDayOfMonth(monthStart.lengthOfMonth()))) {
            throw new BusinessRuleException(String.format(
                    "%s foi admitido a %s: não há recibo de %d/%d, que é anterior à admissão. "
                            + "Corrija a data de admissão na ficha se estiver errada.",
                    employee.getName(), DATE_PT.format(employee.getHireDate()), month, year));
        }
        throw new BusinessRuleException(String.format(
                "O vínculo de %s terminou a %s: não há recibo de %d/%d. "
                        + "Valores ainda em dívida pagam-se pelo acerto final da cessação.",
                employee.getName(), DATE_PT.format(employee.getContractEndDate()), month, year));
    }

    private BigDecimal calculateAbsenceDeduction(Employee employee, int year, int month) {
        BigDecimal baseSalary = employee.getBaseSalary();
        if (baseSalary == null || baseSalary.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        LocalDate periodStart = LocalDate.of(year, month, 1);
        LocalDate periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());
        int unpaidDays = absenceRepository
                .findUnpaidOverlapping(employee.getId(), periodStart, periodEnd,
                        AbsencePayRule.unpaidTypes()).stream()
                .mapToInt(a -> {
                    LocalDate from = a.getStartDate().isBefore(periodStart) ? periodStart : a.getStartDate();
                    LocalDate to = a.getEndDate().isAfter(periodEnd) ? periodEnd : a.getEndDate();
                    return (int) (ChronoUnit.DAYS.between(from, to) + 1);
                })
                .sum();
        if (unpaidDays <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal dailyRate = baseSalary.divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
        return dailyRate.multiply(BigDecimal.valueOf(unpaidDays)).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateNet(Payslip p) {
        BigDecimal gross = p.getBaseSalary().add(p.getAllowances()).add(p.getOvertime()).add(orZero(p.getSalesBonus()));
        BigDecimal deductions = p.getIrpsDeduction().add(p.getInssDeduction())
                .add(p.getOtherDeductions()).add(p.getAbsenceDeduction());
        return gross.subtract(deductions);
    }

    public static BigDecimal orZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    public static PayslipDTO payslipToDTO(Payslip p) {
        BigDecimal gross = p.getBaseSalary().add(p.getAllowances()).add(p.getOvertime()).add(orZero(p.getSalesBonus()));
        BigDecimal totalDeductions = p.getIrpsDeduction().add(p.getInssDeduction())
                .add(p.getOtherDeductions()).add(p.getAbsenceDeduction());
        return new PayslipDTO(
                p.getId(), p.getPayslipNumber(), p.getEmployee().getId(), p.getEmployee().getName(),
                p.getEmployee().getDepartment(), p.getYear(), p.getMonth(), p.getBaseSalary(),
                p.getAllowances(), p.getOvertime(), orZero(p.getSalesBonus()), p.getIrpsDeduction(),
                p.getInssDeduction(), p.getEmployerInss(), p.getTaxableIncome(), p.getOtherDeductions(),
                p.getAbsenceDeduction(), gross, totalDeductions, p.getNetPay(), p.getStatus(),
                p.getPaymentDate(), p.getNotes()
        );
    }
}
