package mz.multicore.erp.modules.performance.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.Payslip;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.PayslipRepository;
import mz.multicore.erp.modules.hr.service.PayrollPeriodService;
import mz.multicore.erp.modules.performance.dto.SalesGoalProgressDTO;
import mz.multicore.erp.modules.performance.model.BonusStatus;
import mz.multicore.erp.modules.performance.model.GoalScope;
import mz.multicore.erp.modules.performance.model.GoalStatus;
import mz.multicore.erp.modules.performance.model.SalesGoal;
import mz.multicore.erp.modules.performance.model.SalesGoalBonus;
import mz.multicore.erp.modules.performance.repository.SalesGoalBonusRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class SalesGoalBonusPayrollService {

    private final SalesGoalBonusRepository bonusRepository;
    private final EmployeeRepository employeeRepository;
    private final PayslipRepository payslipRepository;
    private final PayrollPeriodService payrollPeriodService;
    private final BonusCalculatorEngine bonusCalculatorEngine;
    private final AuditLogService auditLogService;

    public SalesGoalBonusPayrollService(
            SalesGoalBonusRepository bonusRepository,
            EmployeeRepository employeeRepository,
            PayslipRepository payslipRepository,
            PayrollPeriodService payrollPeriodService,
            BonusCalculatorEngine bonusCalculatorEngine,
            AuditLogService auditLogService
    ) {
        this.bonusRepository = bonusRepository;
        this.employeeRepository = employeeRepository;
        this.payslipRepository = payslipRepository;
        this.payrollPeriodService = payrollPeriodService;
        this.bonusCalculatorEngine = bonusCalculatorEngine;
        this.auditLogService = auditLogService;
    }

    public SalesGoalBonus integrateBonusWithPayroll(Long bonusId, int year, int month) {
        SalesGoalBonus bonus = bonusRepository.findById(bonusId)
                .orElseThrow(() -> new BusinessRuleException("Prémio não encontrado."));
        if (bonus.getStatus() != BonusStatus.APPROVED) {
            throw new BusinessRuleException("Apenas prémios no estado APROVADO podem ser integrados na folha salarial.");
        }
        if (bonus.getEmployee() == null) {
            throw new BusinessRuleException("Este prémio não tem colaborador associado para recibo de vencimento.");
        }

        Long companyId = bonus.getCompany().getId();
        if (payrollPeriodService.isClosed(year, month)) {
            throw new BusinessRuleException(String.format(
                    "O período de folha %02d/%d está fechado. Reabra o período ou integre num mês aberto.", month, year));
        }

        Payslip payslip = payslipRepository.findByEmployeeIdAndYearAndMonth(bonus.getEmployee().getId(), year, month)
                .orElseThrow(() -> new BusinessRuleException(String.format(
                        "Não existe recibo de vencimento emitido para %s em %02d/%d.", bonus.getEmployee().getName(), month, year)));
        if ("PAID".equals(payslip.getStatus())) {
            throw new BusinessRuleException("O recibo deste colaborador para este mês já foi pago.");
        }

        BigDecimal currentBonus = payslip.getSalesBonus() != null ? payslip.getSalesBonus() : BigDecimal.ZERO;
        payslip.setSalesBonus(currentBonus.add(bonus.getApprovedAmount()));
        payslip.setNetPay(payslip.getNetPay().add(bonus.getApprovedAmount()));
        payslipRepository.save(payslip);

        bonus.setPayslip(payslip);
        bonus.setStatus(BonusStatus.PAID);
        SalesGoalBonus saved = bonusRepository.save(bonus);

        auditLogService.logEvent(CurrentUserContext.getUsername(), companyId, "BONUS_PAID_IN_PAYSLIP",
                String.format("Prémio de %s MZN integrado no recibo %s (%02d/%d) de %s",
                        bonus.getApprovedAmount(), payslip.getPayslipNumber(), month, year, bonus.getEmployee().getName()));
        return saved;
    }

    public void generateBonusesForAchievedGoal(SalesGoal goal, SalesGoalProgressDTO progress) {
        if (goal == null || goal.getStatus() != GoalStatus.ACHIEVED) return;

        BigDecimal calculatedBonus = bonusCalculatorEngine.calculateBonus(goal, progress.currentRevenue(), progress.currentMargin());
        if (calculatedBonus.compareTo(BigDecimal.ZERO) <= 0) return;

        if (goal.getScope() == GoalScope.EMPLOYEE && goal.getScopeRefId() != null) {
            Employee employee = employeeRepository.findById(goal.getScopeRefId()).orElse(null);
            if (employee != null && bonusRepository.findByGoalIdAndEmployeeId(goal.getId(), employee.getId()).isEmpty()) {
                createBonusRecord(goal, employee, calculatedBonus);
            }
        } else {
            List<Employee> activeEmployees = employeeRepository.findByCompanyIdAndStatus(goal.getCompany().getId(), "ACTIVE");
            if (!activeEmployees.isEmpty()) {
                BigDecimal share = calculatedBonus.divide(BigDecimal.valueOf(activeEmployees.size()), 2, RoundingMode.HALF_UP);
                for (Employee emp : activeEmployees) {
                    if (bonusRepository.findByGoalIdAndEmployeeId(goal.getId(), emp.getId()).isEmpty()) {
                        createBonusRecord(goal, emp, share);
                    }
                }
            }
        }
    }

    private void createBonusRecord(SalesGoal goal, Employee employee, BigDecimal amount) {
        SalesGoalBonus bonus = new SalesGoalBonus();
        bonus.setGoal(goal);
        bonus.setEmployee(employee);
        bonus.setCompany(goal.getCompany());
        bonus.setCalculatedAmount(amount);
        bonus.setApprovedAmount(amount);
        bonus.setStatus(BonusStatus.PENDING);
        bonus.setCreatedBy("SYSTEM");
        bonusRepository.save(bonus);

        auditLogService.logEvent("SYSTEM", goal.getCompany().getId(), "BONUS_CALCULATED",
                String.format("Prémio de %s MZN calculado para meta '%s' (colaborador: %s)",
                        amount, goal.getName(), employee != null ? employee.getName() : "Geral"));
    }
}
