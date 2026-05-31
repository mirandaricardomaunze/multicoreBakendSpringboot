package com.phcpro.modules.fiscal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateWithholdingRequest(
        @NotNull Long companyId,
        @NotNull LocalDate recordDate,
        @NotBlank String beneficiaryName,
        String beneficiaryTaxId,
        @NotBlank String serviceDescription,
        @NotNull @Positive BigDecimal baseAmount,
        @NotNull @PositiveOrZero BigDecimal taxRate,
        @NotBlank String taxCategory
) {}
