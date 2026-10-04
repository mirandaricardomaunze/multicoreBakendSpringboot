package mz.multicore.erp.modules.performance.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import mz.multicore.erp.modules.performance.model.BonusType;
import mz.multicore.erp.modules.performance.model.GoalPeriod;
import mz.multicore.erp.modules.performance.model.GoalScope;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * {@code period} e {@code scope} são opcionais: o {@code SalesGoalService} assume MONTHLY e COMPANY.
 */
public record CreateSalesGoalRequest(
        @NotNull(message = "A empresa é obrigatória.")
        Long companyId,

        @NotBlank(message = "O nome do objectivo é obrigatório.")
        @Size(max = 120, message = "O nome do objectivo não pode exceder 120 caracteres.")
        String name,

        GoalPeriod period,

        @NotNull(message = "A data de início do período é obrigatória.")
        LocalDate periodStart,

        @NotNull(message = "A data de fim do período é obrigatória.")
        LocalDate periodEnd,

        GoalScope scope,
        Long scopeRefId,

        @Size(max = 120, message = "A descrição do âmbito não pode exceder 120 caracteres.")
        String scopeLabel,

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

        Boolean autoApplyBonus
) {}
