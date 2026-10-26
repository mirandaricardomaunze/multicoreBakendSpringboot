package mz.multicore.erp.modules.performance.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.performance.dto.SalesGoalDTO;
import mz.multicore.erp.modules.performance.model.GoalPeriod;
import mz.multicore.erp.modules.performance.model.GoalStatus;
import mz.multicore.erp.modules.performance.model.SalesGoal;
import mz.multicore.erp.modules.performance.repository.SalesGoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Consultas analíticas e projecção de metas comerciais para DTO.
 */
@Service
public class SalesGoalQueryService {

    private final SalesGoalRepository salesGoalRepository;

    public SalesGoalQueryService(SalesGoalRepository salesGoalRepository) {
        this.salesGoalRepository = salesGoalRepository;
    }

    @Transactional(readOnly = true)
    public List<SalesGoalDTO> listGoals(Long companyId, GoalStatus status, GoalPeriod period) {
        List<SalesGoal> list;
        if (status != null && period != null) {
            list = salesGoalRepository.findByCompanyIdAndStatusAndPeriodOrderByPeriodStartDesc(companyId, status, period);
        } else if (status != null) {
            list = salesGoalRepository.findByCompanyIdAndStatusOrderByPeriodStartDesc(companyId, status);
        } else if (period != null) {
            list = salesGoalRepository.findByCompanyIdAndPeriodOrderByPeriodStartDesc(companyId, period);
        } else {
            list = salesGoalRepository.findByCompanyIdOrderByPeriodStartDesc(companyId);
        }
        return list.stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public SalesGoalDTO getGoal(Long id, Long companyId) {
        SalesGoal goal = findGoal(id, companyId);
        return toDTO(goal);
    }

    public SalesGoal findGoal(Long id, Long companyId) {
        return salesGoalRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Meta não encontrada."));
    }

    public SalesGoalDTO toDTO(SalesGoal g) {
        return new SalesGoalDTO(
                g.getId(),
                g.getCompany().getId(),
                g.getName(),
                g.getPeriod(),
                g.getPeriodStart(),
                g.getPeriodEnd(),
                g.getScope(),
                g.getScopeRefId(),
                g.getScopeLabel(),
                g.getTargetRevenue(),
                g.getTargetMargin(),
                g.getTargetMarginPct(),
                g.getBonusType(),
                g.getBonusValue(),
                g.getBonusCap(),
                g.getStatus(),
                g.isAutoApplyBonus(),
                g.getCreatedBy()
        );
    }
}
