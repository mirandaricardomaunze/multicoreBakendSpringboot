package mz.multicore.erp.modules.performance.dto;

import mz.multicore.erp.modules.performance.model.BonusType;
import mz.multicore.erp.modules.performance.model.GoalPeriod;
import mz.multicore.erp.modules.performance.model.GoalScope;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateSalesGoalRequest(
        Long companyId,
        String name,
        GoalPeriod period,
        LocalDate periodStart,
        LocalDate periodEnd,
        GoalScope scope,
        Long scopeRefId,
        String scopeLabel,
        BigDecimal targetRevenue,
        BigDecimal targetMargin,
        BigDecimal targetMarginPct,
        BonusType bonusType,
        BigDecimal bonusValue,
        BigDecimal bonusCap,
        Boolean autoApplyBonus
) {}
