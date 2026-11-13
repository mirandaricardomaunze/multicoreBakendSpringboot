package mz.multicore.erp.modules.performance.service;

import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.hr.service.EmployeeCommandService;
import mz.multicore.erp.modules.hr.service.EmployeeLookup;
import mz.multicore.erp.modules.hr.service.PayrollPeriodService;
import mz.multicore.erp.modules.performance.dto.AdjustBonusRequest;
import mz.multicore.erp.modules.performance.dto.ApproveBonusRequest;
import mz.multicore.erp.modules.performance.dto.SalesGoalBonusDTO;
import mz.multicore.erp.modules.performance.dto.SalesGoalProgressDTO;
import mz.multicore.erp.modules.performance.model.BonusStatus;
import mz.multicore.erp.modules.performance.model.SalesGoal;
import mz.multicore.erp.modules.performance.model.SalesGoalBonus;
import mz.multicore.erp.modules.performance.repository.SalesGoalBonusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestão, apuração e integração na folha salarial de prémios de desempenho comercial.
 */
@Service
public class SalesGoalBonusService {

    private final SalesGoalBonusApprovalService approvalService;
    private final SalesGoalBonusPayrollService payrollService;

    @Autowired
    public SalesGoalBonusService(
            SalesGoalBonusApprovalService approvalService,
            SalesGoalBonusPayrollService payrollService
    ) {
        this.approvalService = approvalService;
        this.payrollService = payrollService;
    }

    public SalesGoalBonusService(
            SalesGoalBonusRepository bonusRepository,
            EmployeeLookup employeeLookup,
            EmployeeCommandService employeeCommandService,
            PayrollPeriodService payrollPeriodService,
            BonusCalculatorEngine bonusCalculatorEngine,
            AuditLogService auditLogService
    ) {
        this(
                new SalesGoalBonusApprovalService(bonusRepository, auditLogService),
                new SalesGoalBonusPayrollService(
                        bonusRepository, employeeLookup, employeeCommandService,
                        payrollPeriodService, bonusCalculatorEngine, auditLogService)
        );
    }

    @Transactional(readOnly = true)
    public List<SalesGoalBonusDTO> listBonuses(Long companyId, BonusStatus status) {
        return approvalService.listBonuses(companyId, status);
    }

    @Transactional
    public SalesGoalBonusDTO approveBonus(Long id, ApproveBonusRequest request) {
        PermissionGuard.requireAdmin("aprovar prémio de desempenho");
        return approvalService.approveBonus(id, request);
    }

    @Transactional
    public SalesGoalBonusDTO adjustBonus(Long id, AdjustBonusRequest request) {
        PermissionGuard.requireAdmin("ajustar prémio de desempenho");
        return approvalService.adjustBonus(id, request);
    }

    @Transactional
    public SalesGoalBonusDTO integrateBonusWithPayroll(Long bonusId, int year, int month) {
        PermissionGuard.requireAdmin("integrar prémio de desempenho na folha salarial");
        return approvalService.toDTO(payrollService.integrateBonusWithPayroll(bonusId, year, month));
    }

    public void generateBonusesForAchievedGoal(SalesGoal goal, SalesGoalProgressDTO progress) {
        payrollService.generateBonusesForAchievedGoal(goal, progress);
    }

    public SalesGoalBonusDTO toDTO(SalesGoalBonus b) {
        return approvalService.toDTO(b);
    }
}
