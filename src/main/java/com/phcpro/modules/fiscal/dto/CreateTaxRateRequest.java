package com.phcpro.modules.fiscal.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreateTaxRateRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String type,
        @NotNull @PositiveOrZero @DecimalMax("9.9999") BigDecimal rate,
        String legalBasis
) {}
