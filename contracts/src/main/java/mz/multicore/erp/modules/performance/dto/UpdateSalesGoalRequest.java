package mz.multicore.erp.modules.performance.dto;

import mz.multicore.erp.modules.performance.model.BonusType;
import mz.multicore.erp.modules.performance.model.GoalStatus;

import java.math.BigDecimal;

public record UpdateSalesGoalRequest(
        String name,
        BigDecimal targetRevenue,
        BigDecimal targetMargin,
        BigDecimal targetMarginPct,
        BonusType bonusType,
        BigDecimal bonusValue,
        BigDecimal bonusCap,
        Boolean autoApplyBonus,
        GoalStatus status
) {}
