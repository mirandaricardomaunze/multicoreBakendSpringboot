package mz.multicore.erp.modules.performance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AdjustBonusRequest(
        @NotNull(message = "O valor ajustado é obrigatório.")
        @PositiveOrZero(message = "O valor ajustado não pode ser negativo.")
        BigDecimal adjustedAmount,

        @Size(max = 500, message = "A justificação não pode exceder 500 caracteres.")
        String justification
) {}
