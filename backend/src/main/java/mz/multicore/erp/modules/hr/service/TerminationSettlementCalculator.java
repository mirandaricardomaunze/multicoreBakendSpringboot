package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.hr.dto.CreateTerminationRequest;
import mz.multicore.erp.modules.hr.dto.PayrollDeductionDTO;
import mz.multicore.erp.modules.hr.dto.TerminationDTO;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.EmploymentContract;
import mz.multicore.erp.modules.hr.model.TerminationReason;
import mz.multicore.erp.modules.hr.model.TerminationSettlementLine;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.VacationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TerminationSettlementCalculator {
    public static final BigDecimal MONTHLY_DIVISOR = BigDecimal.valueOf(30);
    public static final BigDecimal MONTHS_IN_YEAR = BigDecimal.valueOf(12);
    public static final int FALLBACK_ANNUAL_VACATION_DAYS = 22;

    public record Settlement(EmploymentContract contract, TerminationReason reason,
                             List<TerminationSettlementLine> lines, List<String> warnings) {}

    private final EmployeeRepository employeeRepository;
    private final EmploymentContractService contractService;
    private final SalaryHistoryService salaryHistoryService;
    private final VacationRepository vacationRepository;
    private final HrPolicyService hrPolicyService;
    private final PayrollDeductionService payrollDeductionService;

    public TerminationSettlementCalculator(EmployeeRepository employeeRepository,
                                           EmploymentContractService contractService,
                                           SalaryHistoryService salaryHistoryService,
                                           VacationRepository vacationRepository,
                                           HrPolicyService hrPolicyService,
                                           PayrollDeductionService payrollDeductionService) {
        this.employeeRepository = employeeRepository;
        this.contractService = contractService;
        this.salaryHistoryService = salaryHistoryService;
        this.vacationRepository = vacationRepository;
        this.hrPolicyService = hrPolicyService;
        this.payrollDeductionService = payrollDeductionService;
    }

    @Transactional(readOnly = true)
    public TerminationDTO preview(CreateTerminationRequest request) {
        PermissionGuard.requireManagerOrAdmin("cessar vínculos e pagar acertos finais");
        Employee employee = findEmployee(request.employeeId());
        Settlement settlement = compute(employee, request);
        return TerminationQueryService.previewDTO(employee, settlement, request);
    }

    public Settlement compute(Employee employee, CreateTerminationRequest request) {
        TerminationReason reason = parseReason(request.reason());
        LocalDate exit = request.terminationDate();
        List<TerminationSettlementLine> lines = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        EmploymentContract contract = contractService
                .findContractInPeriod(employee.getId(), exit, exit).orElse(null);
        if (contract == null) {
            warnings.add("Este colaborador não tem contrato vigente à data da saída. A cessação é "
                    + "registada na mesma, mas nada fica ligado a um contrato.");
        }

        BigDecimal salary = salaryHistoryService.salaryOn(employee.getId(), exit)
                .orElseGet(() -> employee.getBaseSalary() == null ? BigDecimal.ZERO : employee.getBaseSalary());
        BigDecimal dailyRate = salary.divide(MONTHLY_DIVISOR, 2, RoundingMode.HALF_UP);

        addSalaryUntilExit(lines, salary, exit);
        addProportionalThirteenth(lines, salary, employee, exit);
        addUnusedVacation(lines, dailyRate, employee, exit, warnings);
        addCompensation(lines, request);
        addUnservedNotice(lines, dailyRate, reason, request, exit, warnings);
        addOutstandingDeductions(lines, employee);

        return new Settlement(contract, reason, lines, warnings);
    }

    private void addSalaryUntilExit(List<TerminationSettlementLine> lines, BigDecimal salary, LocalDate exit) {
        int daysWorked = exit.getDayOfMonth();
        int daysInMonth = exit.lengthOfMonth();
        BigDecimal amount = salary.multiply(BigDecimal.valueOf(daysWorked))
                .divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);
        lines.add(line(String.format("Salário de %02d/%d até %s (%d de %d dias)",
                exit.getMonthValue(), exit.getYear(), exit, daysWorked, daysInMonth), amount, true));
    }

    private void addProportionalThirteenth(List<TerminationSettlementLine> lines, BigDecimal salary,
                                           Employee employee, LocalDate exit) {
        LocalDate yearStart = LocalDate.of(exit.getYear(), 1, 1);
        LocalDate from = employee.getHireDate() != null && employee.getHireDate().isAfter(yearStart)
                ? employee.getHireDate() : yearStart;
        int months = Math.max(0, exit.getMonthValue() - from.getMonthValue() + 1);
        if (months <= 0) return;
        BigDecimal amount = salary.multiply(BigDecimal.valueOf(months))
                .divide(MONTHS_IN_YEAR, 2, RoundingMode.HALF_UP);
        lines.add(line(String.format("13.º mês proporcional (%d de 12 meses de %d)",
                months, exit.getYear()), amount, true));
    }

    private void addUnusedVacation(List<TerminationSettlementLine> lines, BigDecimal dailyRate,
                                   Employee employee, LocalDate exit, List<String> warnings) {
        int completedYears = employee.getHireDate() == null
                ? 0 : (int) ChronoUnit.YEARS.between(employee.getHireDate(), exit);
        Optional<Integer> configured = hrPolicyService.annualVacationDays(completedYears, exit);
        int entitlement = configured.orElse(FALLBACK_ANNUAL_VACATION_DAYS);
        if (configured.isEmpty()) {
            warnings.add(String.format(
                    "O direito anual de férias não está configurado para %d ano(s) de casa. O acerto "
                            + "usa o valor histórico de %d dias — confirme-o com o contabilista em "
                            + "RH › Valores Legais.", completedYears, FALLBACK_ANNUAL_VACATION_DAYS));
        }
        int taken = vacationRepository.sumReservedDays(employee.getId(), exit.getYear());
        int unused = entitlement - taken;
        if (unused <= 0) return;
        BigDecimal amount = dailyRate.multiply(BigDecimal.valueOf(unused)).setScale(2, RoundingMode.HALF_UP);
        lines.add(line(String.format("Férias vencidas e não gozadas (%d de %d dias de %d)",
                unused, entitlement, exit.getYear()), amount, true));
    }

    private void addCompensation(List<TerminationSettlementLine> lines, CreateTerminationRequest request) {
        if (request.compensationAmount() == null || request.compensationAmount().signum() <= 0) return;
        lines.add(line("Compensação por cessação", request.compensationAmount(), true));
    }

    private void addUnservedNotice(List<TerminationSettlementLine> lines, BigDecimal dailyRate,
                                   TerminationReason reason, CreateTerminationRequest request,
                                   LocalDate exit, List<String> warnings) {
        if (request.noticeServed() || !reason.isNoticeOwedByEmployee()) return;
        Optional<Integer> noticeDays = hrPolicyService.noticeDays(false, exit);
        if (noticeDays.isEmpty()) {
            warnings.add("O aviso prévio não foi cumprido, mas o número de dias não está configurado "
                    + "— nada foi descontado. Configure-o em RH › Valores Legais e refaça o acerto.");
            return;
        }
        BigDecimal amount = dailyRate.multiply(BigDecimal.valueOf(noticeDays.get())).setScale(2, RoundingMode.HALF_UP);
        lines.add(line(String.format("Aviso prévio não cumprido (%d dias)", noticeDays.get()), amount, false));
    }

    private void addOutstandingDeductions(List<TerminationSettlementLine> lines, Employee employee) {
        for (PayrollDeductionDTO deduction : payrollDeductionService.outstandingFor(employee.getId())) {
            lines.add(line(deduction.kindLabel() + " por liquidar — " + deduction.description(),
                    deduction.outstandingAmount(), false));
        }
    }

    private static TerminationSettlementLine line(String description, BigDecimal amount, boolean earning) {
        TerminationSettlementLine line = new TerminationSettlementLine();
        line.setDescription(description);
        line.setAmount(amount);
        line.setEarning(earning);
        return line;
    }

    private TerminationReason parseReason(String raw) {
        try {
            return TerminationReason.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Motivo de cessação desconhecido: " + raw);
        }
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findByIdAndCompanyId(id, CurrentUserContext.requireCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Colaborador não encontrado na empresa activa."));
    }
}
