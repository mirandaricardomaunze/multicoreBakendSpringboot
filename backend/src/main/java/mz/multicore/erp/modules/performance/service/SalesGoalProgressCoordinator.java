package mz.multicore.erp.modules.performance.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.performance.dto.SalesGoalProgressDTO;
import mz.multicore.erp.modules.performance.model.GoalStatus;
import mz.multicore.erp.modules.performance.model.SalesGoal;
import mz.multicore.erp.modules.performance.repository.SalesGoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Apuração de progresso de metas comerciais e transição de estado para alcançado/perdido.
 */
@Service
public class SalesGoalProgressCoordinator {

    private final SalesGoalRepository salesGoalRepository;
    private final GoalProgressEngine progressEngine;
    private final SalesGoalBonusService bonusService;
    private final AuditLogService auditLogService;

    public SalesGoalProgressCoordinator(SalesGoalRepository salesGoalRepository,
                                        GoalProgressEngine progressEngine,
                                        SalesGoalBonusService bonusService,
                                        AuditLogService auditLogService) {
        this.salesGoalRepository = salesGoalRepository;
        this.progressEngine = progressEngine;
        this.bonusService = bonusService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public SalesGoalProgressDTO getGoalProgress(Long id) {
        SalesGoal goal = salesGoalRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Meta não encontrada."));
        return progressEngine.calculateProgress(goal, LocalDate.now());
    }

    @Transactional
    public List<SalesGoalProgressDTO> getActiveGoalsSummary(Long companyId) {
        List<SalesGoal> active = salesGoalRepository.findByCompanyIdAndStatus(companyId, GoalStatus.ACTIVE);
        List<SalesGoalProgressDTO> summaries = new ArrayList<>();

        for (SalesGoal goal : active) {
            SalesGoalProgressDTO progress = progressEngine.calculateProgress(goal, LocalDate.now());
            summaries.add(progress);

            if (progress.progressPct().compareTo(BigDecimal.valueOf(100)) >= 0) {
                goal.setStatus(GoalStatus.ACHIEVED);
                salesGoalRepository.save(goal);

                auditLogService.logEvent("SYSTEM", companyId, "GOAL_ACHIEVED",
                        String.format("Meta '%s' atingida com %s%% de progresso", goal.getName(), progress.progressPct()));

                if (goal.isAutoApplyBonus()) {
                    bonusService.generateBonusesForAchievedGoal(goal, progress);
                }
            } else if (LocalDate.now().isAfter(goal.getPeriodEnd())) {
                goal.setStatus(GoalStatus.MISSED);
                salesGoalRepository.save(goal);

                auditLogService.logEvent("SYSTEM", companyId, "GOAL_MISSED",
                        String.format("Meta '%s' não atingida ao fim do período (%s%% de progresso)",
                                goal.getName(), progress.progressPct()));
            }
        }

        return summaries;
    }
}
