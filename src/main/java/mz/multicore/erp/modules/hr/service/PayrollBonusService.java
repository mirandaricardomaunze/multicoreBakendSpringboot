package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.hr.dto.ThirteenthMonthDTO;
import mz.multicore.erp.modules.hr.dto.VacationAllowanceDTO;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.PayrollBonus;
import mz.multicore.erp.modules.hr.model.Vacation;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.PayrollBonusRepository;
import mz.multicore.erp.modules.hr.repository.VacationRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Apuramento de subsídios legais da folha moçambicana — 13.º mês (subsídio de Natal)
 * proporcional ao tempo de serviço no ano e subsídio de férias por pedido aprovado.
 * Apenas cálculo (read-only); o pagamento/persistência é tratado fora deste serviço.
 */
@Service
public class PayrollBonusService {

    /** Divisor mensal padrão para apurar o valor/dia a partir do salário base. */
    private static final BigDecimal MONTHLY_DIVISOR = BigDecimal.valueOf(30);
    private static final BigDecimal MONTHS_IN_YEAR = BigDecimal.valueOf(12);

    private static final String THIRTEENTH_MONTH = "THIRTEENTH_MONTH";
    private static final String VACATION_ALLOWANCE = "VACATION_ALLOWANCE";

    private final EmployeeRepository employeeRepository;
    private final VacationRepository vacationRepository;
    private final PayrollBonusRepository bonusRepository;
    private final AuditLogService auditLogService;
    private final FinanceService financeService;

    public PayrollBonusService(
            EmployeeRepository employeeRepository,
            VacationRepository vacationRepository,
            PayrollBonusRepository bonusRepository,
            AuditLogService auditLogService,
            @Lazy FinanceService financeService
    ) {
        this.employeeRepository = employeeRepository;
        this.vacationRepository = vacationRepository;
        this.bonusRepository = bonusRepository;
        this.auditLogService = auditLogService;
        this.financeService = financeService;
    }

    @Transactional(readOnly = true)
    public ThirteenthMonthDTO thirteenthMonth(int year) {
        List<ThirteenthMonthDTO.ThirteenthMonthLineDTO> lines =
                employeeRepository.findByCompanyIdOrderByName(currentCompanyId()).stream()
                        .map(e -> line(e, year))
                        .filter(l -> l.monthsWorked() > 0 && l.amount().signum() > 0)
                        .toList();
        BigDecimal total = lines.stream()
                .map(ThirteenthMonthDTO.ThirteenthMonthLineDTO::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ThirteenthMonthDTO(year, total, lines);
    }

    @Transactional(readOnly = true)
    public VacationAllowanceDTO vacationAllowance(Long vacationId) {
        Vacation v = vacationRepository.findByIdAndEmployeeCompanyId(vacationId, currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Pedido de férias não encontrado."));
        if (!"APPROVED".equals(v.getStatus())) {
            throw new BusinessRuleException("Apenas férias aprovadas têm subsídio de férias.");
        }
        return allowanceFor(v);
    }

    private VacationAllowanceDTO allowanceFor(Vacation v) {
        Employee e = v.getEmployee();
        BigDecimal base = orZero(e.getBaseSalary());
        BigDecimal dailyRate = base.divide(MONTHLY_DIVISOR, 2, RoundingMode.HALF_UP);
        BigDecimal amount = dailyRate.multiply(BigDecimal.valueOf(v.getTotalDays())).setScale(2, RoundingMode.HALF_UP);
        return new VacationAllowanceDTO(v.getId(), e.getId(), e.getName(), v.getTotalDays(), dailyRate, amount);
    }

    // ─── Pagamento (persiste + tesouraria + auditoria, idempotente) ──────────

    /**
     * Paga o 13.º mês do ano a todos os colaboradores elegíveis ainda não pagos. Idempotente:
     * reexecutar não paga duas vezes. Devolve apenas as linhas efectivamente pagas nesta execução.
     */
    @Transactional
    public ThirteenthMonthDTO payThirteenthMonth(int year) {
        ensureHrManager();
        List<ThirteenthMonthDTO.ThirteenthMonthLineDTO> paid = new ArrayList<>();
        for (Employee e : employeeRepository.findByCompanyIdOrderByName(currentCompanyId())) {
            ThirteenthMonthDTO.ThirteenthMonthLineDTO line = line(e, year);
            if (line.monthsWorked() <= 0 || line.amount().signum() <= 0) {
                continue;
            }
            if (bonusRepository.existsByEmployeeIdAndBonusTypeAndYear(e.getId(), THIRTEENTH_MONTH, year)) {
                continue; // já pago — não duplicar
            }
            persistAndPay(e, THIRTEENTH_MONTH, year, 0L, line.amount(),
                    String.format("13.º mês %d - %s", year, e.getName()));
            paid.add(line);
        }
        BigDecimal total = paid.stream()
                .map(ThirteenthMonthDTO.ThirteenthMonthLineDTO::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        auditLogService.logCurrent("BONUS_13TH_PAY", String.format(
                "13.º mês %d pago a %d colaborador(es), total %s", year, paid.size(), total));
        return new ThirteenthMonthDTO(year, total, paid);
    }

    /** Paga o subsídio de férias de um pedido aprovado. Bloqueia se já tiver sido pago. */
    @Transactional
    public VacationAllowanceDTO payVacationAllowance(Long vacationId) {
        ensureHrManager();
        Vacation v = vacationRepository.findByIdAndEmployeeCompanyId(vacationId, currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Pedido de férias não encontrado."));
        if (!"APPROVED".equals(v.getStatus())) {
            throw new BusinessRuleException("Apenas férias aprovadas têm subsídio de férias.");
        }
        if (bonusRepository.existsByBonusTypeAndReferenceId(VACATION_ALLOWANCE, v.getId())) {
            throw new BusinessRuleException("O subsídio de férias deste pedido já foi pago.");
        }
        VacationAllowanceDTO dto = allowanceFor(v);
        persistAndPay(v.getEmployee(), VACATION_ALLOWANCE, v.getYearReference(), v.getId(), dto.amount(),
                String.format("Subsídio de férias #%d - %s", v.getId(), v.getEmployee().getName()));
        auditLogService.logCurrent("BONUS_VACATION_PAY", String.format(
                "Subsídio de férias #%d pago a %s, valor %s", v.getId(), v.getEmployee().getName(), dto.amount()));
        return dto;
    }

    private void persistAndPay(Employee employee, String type, int year, long referenceId,
                               BigDecimal amount, String description) {
        PayrollBonus bonus = new PayrollBonus();
        bonus.setEmployee(employee);
        bonus.setBonusType(type);
        bonus.setYear(year);
        bonus.setReferenceId(referenceId);
        bonus.setAmount(amount);
        bonus.setStatus("PAID");
        bonus.setPaymentDate(LocalDate.now());
        bonusRepository.save(bonus);
        financeService.registerAutoPayout(amount, description);
    }

    private void ensureHrManager() {
        String role = CurrentUserContext.getRole();
        if (!"ADMIN".equalsIgnoreCase(role) && !"MANAGER".equalsIgnoreCase(role)) {
            throw new BusinessRuleException("Apenas gestores ou administradores podem pagar subsídios.");
        }
    }

    private ThirteenthMonthDTO.ThirteenthMonthLineDTO line(Employee e, int year) {
        int months = monthsWorkedInYear(e, year);
        BigDecimal base = orZero(e.getBaseSalary());
        BigDecimal amount = base
                .multiply(BigDecimal.valueOf(months))
                .divide(MONTHS_IN_YEAR, 2, RoundingMode.HALF_UP);
        return new ThirteenthMonthDTO.ThirteenthMonthLineDTO(
                e.getId(), e.getEmployeeNumber(), e.getName(), base, months, amount);
    }

    /** Meses completos de serviço dentro do ano, limitado a [0, 12]. */
    private int monthsWorkedInYear(Employee e, int year) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);
        LocalDate hire = e.getHireDate();
        LocalDate contractEnd = e.getContractEndDate();
        if (hire != null && hire.isAfter(yearEnd)) {
            return 0; // admitido depois do ano
        }
        if (contractEnd != null && contractEnd.isBefore(yearStart)) {
            return 0; // contrato terminou antes do ano
        }
        int startMonth = (hire != null && !hire.isBefore(yearStart)) ? hire.getMonthValue() : 1;
        int endMonth = (contractEnd != null && !contractEnd.isAfter(yearEnd)) ? contractEnd.getMonthValue() : 12;
        return Math.max(0, Math.min(12, endMonth - startMonth + 1));
    }

    private BigDecimal orZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private Long currentCompanyId() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (companyId == null) {
            throw new BusinessRuleException("Selecione uma empresa ativa.");
        }
        return companyId;
    }
}
