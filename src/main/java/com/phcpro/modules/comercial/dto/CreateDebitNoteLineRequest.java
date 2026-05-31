package com.phcpro.modules.comercial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreateDebitNoteLineRequest(
        @NotBlank String description,
        @NotNull @PositiveOrZero BigDecimal amount,
        @PositiveOrZero BigDecimal taxRate
) {}
