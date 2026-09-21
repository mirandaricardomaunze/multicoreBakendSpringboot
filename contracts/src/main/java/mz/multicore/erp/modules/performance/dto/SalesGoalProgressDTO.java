package mz.multicore.erp.modules.performance.dto;

import mz.multicore.erp.modules.performance.model.AlertLevel;
import mz.multicore.erp.modules.performance.model.GoalScope;

import java.math.BigDecimal;

public record SalesGoalProgressDTO(
        Long goalId,
        String goalName,
        GoalScope scope,
        String scopeLabel,
        BigDecimal targetRevenue,
        BigDecimal currentRevenue,
        BigDecimal targetMargin,
        BigDecimal currentMargin,
        BigDecimal progressPct,
        long daysElapsed,
        long daysTotal,
        BigDecimal projectedRevenue,
        boolean isOnTrack,
        BigDecimal bonusEstimate,
        AlertLevel alertLevel
) {}
