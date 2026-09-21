package mz.multicore.erp.modules.performance.dto;

import mz.multicore.erp.modules.performance.model.BonusType;
import mz.multicore.erp.modules.performance.model.GoalPeriod;
import mz.multicore.erp.modules.performance.model.GoalScope;
import mz.multicore.erp.modules.performance.model.GoalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesGoalDTO(
        Long id,
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
        GoalStatus status,
        boolean autoApplyBonus,
        String createdBy
) {}
