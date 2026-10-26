package mz.multicore.erp.modules.performance.service;

import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.PayslipRepository;
import mz.multicore.erp.modules.hr.service.PayrollPeriodService;
import mz.multicore.erp.modules.performance.dto.AdjustBonusRequest;
import mz.multicore.erp.modules.performance.dto.ApproveBonusRequest;
import mz.multicore.erp.modules.performance.dto.CreateSalesGoalRequest;
import mz.multicore.erp.modules.performance.dto.SalesGoalBonusDTO;
import mz.multicore.erp.modules.performance.dto.SalesGoalDTO;
import mz.multicore.erp.modules.performance.dto.SalesGoalProgressDTO;
import mz.multicore.erp.modules.performance.dto.UpdateSalesGoalRequest;
import mz.multicore.erp.modules.performance.model.BonusStatus;
import mz.multicore.erp.modules.performance.model.GoalPeriod;
import mz.multicore.erp.modules.performance.model.GoalStatus;
import mz.multicore.erp.modules.performance.model.SalesGoal;
import mz.multicore.erp.modules.performance.repository.SalesGoalBonusRepository;
import mz.multicore.erp.modules.performance.repository.SalesGoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestão de metas comerciais e apuração de prémios de desempenho.
 * Orquestrador desacoplado delegando para serviços especializados.
 */
@Service
public class SalesGoalService {

    private final SalesGoalLifecycleService lifecycleService;
    private final SalesGoalProgressCoordinator progressCoordinator;
    private final SalesGoalBonusService bonusService;
    private final SalesGoalQueryService queryService;

    public SalesGoalService(SalesGoalLifecycleService lifecycleService,
                            SalesGoalProgressCoordinator progressCoordinator,
                            SalesGoalBonusService bonusService,
                            SalesGoalQueryService queryService) {
        this.lifecycleService = lifecycleService;
        this.progressCoordinator = progressCoordinator;
        this.bonusService = bonusService;
        this.queryService = queryService;
    }

    /** Construtor para retrocompatibilidade com testes legados. */
    public SalesGoalService(SalesGoalRepository salesGoalRepository,
                            SalesGoalBonusRepository bonusRepository,
                            CompanyRepository companyRepository,
                            EmployeeRepository employeeRepository,
                            PayslipRepository payslipRepository,
                            PayrollPeriodService payrollPeriodService,
                            GoalProgressEngine progressEngine,
                            BonusCalculatorEngine bonusCalculatorEngine,
                            AuditLogService auditLogService) {
        SalesGoalQueryService qService = new SalesGoalQueryService(salesGoalRepository);
        SalesGoalLifecycleService lService = new SalesGoalLifecycleService(
                salesGoalRepository, companyRepository, employeeRepository, qService, auditLogService);
        SalesGoalBonusService bService = new SalesGoalBonusService(
                bonusRepository, employeeRepository, payslipRepository, payrollPeriodService,
                bonusCalculatorEngine, auditLogService);
        SalesGoalProgressCoordinator pCoordinator = new SalesGoalProgressCoordinator(
                salesGoalRepository, progressEngine, bService, auditLogService);

        this.queryService = qService;
        this.lifecycleService = lService;
        this.bonusService = bService;
        this.progressCoordinator = pCoordinator;
    }

    // ─── Metas Comerciais ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SalesGoalDTO> listGoals(Long companyId, GoalStatus status, GoalPeriod period) {
        return queryService.listGoals(companyId, status, period);
    }

    @Transactional(readOnly = true)
    public SalesGoalDTO getGoal(Long id, Long companyId) {
        return queryService.getGoal(id, companyId);
    }

    @Transactional
    public SalesGoalDTO createGoal(CreateSalesGoalRequest request) {
        return lifecycleService.createGoal(request);
    }

    @Transactional
    public SalesGoalDTO updateGoal(Long id, UpdateSalesGoalRequest request) {
        return lifecycleService.updateGoal(id, request);
    }

    @Transactional
    public void cancelGoal(Long id) {
        lifecycleService.cancelGoal(id);
    }

    // ─── Progresso de Metas ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public SalesGoalProgressDTO getGoalProgress(Long id) {
        return progressCoordinator.getGoalProgress(id);
    }

    @Transactional
    public List<SalesGoalProgressDTO> getActiveGoalsSummary(Long companyId) {
        return progressCoordinator.getActiveGoalsSummary(companyId);
    }

    @Transactional
    public void generateBonusesForAchievedGoal(SalesGoal goal, SalesGoalProgressDTO progress) {
        bonusService.generateBonusesForAchievedGoal(goal, progress);
    }

    // ─── Prémios e Bónus ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SalesGoalBonusDTO> listBonuses(Long companyId, BonusStatus status) {
        return bonusService.listBonuses(companyId, status);
    }

    @Transactional
    public SalesGoalBonusDTO approveBonus(Long id, ApproveBonusRequest request) {
        return bonusService.approveBonus(id, request);
    }

    @Transactional
    public SalesGoalBonusDTO adjustBonus(Long id, AdjustBonusRequest request) {
        return bonusService.adjustBonus(id, request);
    }

    @Transactional
    public SalesGoalBonusDTO integrateBonusWithPayroll(Long bonusId, int year, int month) {
        return bonusService.integrateBonusWithPayroll(bonusId, year, month);
    }
}
