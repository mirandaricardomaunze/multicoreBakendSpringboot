package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.hr.dto.ThirteenthMonthDTO;
import mz.multicore.erp.modules.hr.dto.VacationAllowanceDTO;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.Vacation;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.VacationRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Component
public class PayrollBonusCalculationService {

    private static final BigDecimal MONTHLY_DIVISOR = BigDecimal.valueOf(30);
    private static final BigDecimal MONTHS_IN_YEAR = BigDecimal.valueOf(12);

    private final EmployeeRepository employeeRepository;
    private final VacationRepository vacationRepository;

    public PayrollBonusCalculationService(EmployeeRepository employeeRepository,
                                          VacationRepository vacationRepository) {
        this.employeeRepository = employeeRepository;
        this.vacationRepository = vacationRepository;
    }

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

    public VacationAllowanceDTO vacationAllowance(Long vacationId) {
        Vacation v = requireApprovedVacation(vacationId);
        return allowanceFor(v);
    }

    public Vacation requireApprovedVacation(Long vacationId) {
        Vacation v = vacationRepository.findByIdAndEmployeeCompanyId(vacationId, currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Pedido de férias não encontrado."));
        if (!"APPROVED".equals(v.getStatus())) {
            throw new BusinessRuleException("Apenas férias aprovadas têm subsídio de férias.");
        }
        return v;
    }

    public VacationAllowanceDTO allowanceFor(Vacation v) {
        Employee e = v.getEmployee();
        BigDecimal base = orZero(e.getBaseSalary());
        BigDecimal dailyRate = base.divide(MONTHLY_DIVISOR, 2, RoundingMode.HALF_UP);
        BigDecimal amount = dailyRate.multiply(BigDecimal.valueOf(v.getTotalDays())).setScale(2, RoundingMode.HALF_UP);
        return new VacationAllowanceDTO(v.getId(), e.getId(), e.getName(), v.getTotalDays(), dailyRate, amount);
    }

    public ThirteenthMonthDTO.ThirteenthMonthLineDTO line(Employee e, int year) {
        int months = monthsWorkedInYear(e, year);
        BigDecimal base = orZero(e.getBaseSalary());
        BigDecimal amount = base
                .multiply(BigDecimal.valueOf(months))
                .divide(MONTHS_IN_YEAR, 2, RoundingMode.HALF_UP);
        return new ThirteenthMonthDTO.ThirteenthMonthLineDTO(
                e.getId(), e.getEmployeeNumber(), e.getName(), base, months, amount);
    }

    public int monthsWorkedInYear(Employee e, int year) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);
        LocalDate hire = e.getHireDate();
        LocalDate contractEnd = e.getContractEndDate();
        if (hire != null && hire.isAfter(yearEnd)) {
            return 0;
        }
        if (contractEnd != null && contractEnd.isBefore(yearStart)) {
            return 0;
        }
        int startMonth = (hire != null && !hire.isBefore(yearStart)) ? hire.getMonthValue() : 1;
        int endMonth = (contractEnd != null && !contractEnd.isAfter(yearEnd)) ? contractEnd.getMonthValue() : 12;
        return Math.max(0, Math.min(12, endMonth - startMonth + 1));
    }

    public Long currentCompanyId() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (companyId == null) {
            throw new BusinessRuleException("Selecione uma empresa ativa.");
        }
        return companyId;
    }

    private BigDecimal orZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
