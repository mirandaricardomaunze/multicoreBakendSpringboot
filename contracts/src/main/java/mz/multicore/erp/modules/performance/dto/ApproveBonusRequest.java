package mz.multicore.erp.modules.performance.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ApproveBonusRequest(
        @PositiveOrZero(message = "O valor aprovado não pode ser negativo.")
        BigDecimal approvedAmount,

        @Size(max = 500, message = "A justificação não pode exceder 500 caracteres.")
        String justification
) {}
