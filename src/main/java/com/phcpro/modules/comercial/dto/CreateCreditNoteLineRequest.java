package com.phcpro.modules.comercial.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreateCreditNoteLineRequest(
        @NotNull Long productId,
        @NotNull @Positive BigDecimal quantity,
        @NotNull @PositiveOrZero BigDecimal unitPrice,
        @PositiveOrZero BigDecimal taxRate,
        String batchNumber
) {}
