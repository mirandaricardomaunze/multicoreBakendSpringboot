package mz.multicore.erp.modules.performance.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import mz.multicore.erp.modules.performance.model.BonusType;
import mz.multicore.erp.modules.performance.model.GoalStatus;

import java.math.BigDecimal;

public record UpdateSalesGoalRequest(
        @Size(max = 120, message = "O nome do objectivo não pode exceder 120 caracteres.")
        String name,

        @PositiveOrZero(message = "A meta de receita não pode ser negativa.")
        BigDecimal targetRevenue,

        @PositiveOrZero(message = "A meta de margem não pode ser negativa.")
        BigDecimal targetMargin,

        @DecimalMin(value = "0.00", message = "A margem percentual não pode ser negativa.")
        @DecimalMax(value = "100.00", message = "A margem percentual não pode exceder 100%.")
        BigDecimal targetMarginPct,

        BonusType bonusType,

        @PositiveOrZero(message = "O valor do bónus não pode ser negativo.")
        BigDecimal bonusValue,

        @PositiveOrZero(message = "O tecto do bónus não pode ser negativo.")
        BigDecimal bonusCap,

        Boolean autoApplyBonus,
        GoalStatus status
) {}
