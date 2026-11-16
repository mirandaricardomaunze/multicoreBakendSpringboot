package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.hr.dto.ThirteenthMonthDTO;
import mz.multicore.erp.modules.hr.dto.VacationAllowanceDTO;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.PayrollBonus;
import mz.multicore.erp.modules.hr.model.Vacation;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.PayrollBonusRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class PayrollBonusPaymentService {

    private static final String THIRTEENTH_MONTH = "THIRTEENTH_MONTH";
    private static final String VACATION_ALLOWANCE = "VACATION_ALLOWANCE";

    private final EmployeeRepository employeeRepository;
    private final PayrollBonusRepository bonusRepository;
    private final AuditLogService auditLogService;
    private final FinanceService financeService;
    private final PayrollBonusCalculationService calculationService;

    public PayrollBonusPaymentService(EmployeeRepository employeeRepository,
                                      PayrollBonusRepository bonusRepository,
                                      AuditLogService auditLogService,
                                      @Lazy FinanceService financeService,
                                      PayrollBonusCalculationService calculationService) {
        this.employeeRepository = employeeRepository;
        this.bonusRepository = bonusRepository;
        this.auditLogService = auditLogService;
        this.financeService = financeService;
        this.calculationService = calculationService;
    }

    public ThirteenthMonthDTO payThirteenthMonth(int year) {
        List<ThirteenthMonthDTO.ThirteenthMonthLineDTO> paid = new ArrayList<>();
        for (Employee e : employeeRepository.findByCompanyIdOrderByName(calculationService.currentCompanyId())) {
            ThirteenthMonthDTO.ThirteenthMonthLineDTO line = calculationService.line(e, year);
            if (line.monthsWorked() <= 0 || line.amount().signum() <= 0) {
                continue;
            }
            if (bonusRepository.existsByEmployeeIdAndBonusTypeAndYear(e.getId(), THIRTEENTH_MONTH, year)) {
                continue;
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

    public VacationAllowanceDTO payVacationAllowance(Long vacationId) {
        Vacation v = calculationService.requireApprovedVacation(vacationId);
        if (bonusRepository.existsByBonusTypeAndReferenceId(VACATION_ALLOWANCE, v.getId())) {
            throw new BusinessRuleException("O subsídio de férias deste pedido já foi pago.");
        }
        VacationAllowanceDTO dto = calculationService.allowanceFor(v);
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
        financeService.registerAutoPayout(amount, description, mz.multicore.erp.architecture.events.PayoutPurpose.STAFF_COST);
    }
}
