package com.phcpro.modules.hr.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SavePayrollTaxConfigRequest(
        @NotBlank String name,
        @NotNull LocalDate effectiveFrom,
        LocalDate effectiveTo,
        @NotNull @PositiveOrZero BigDecimal employeeInssRate,
        @NotNull @PositiveOrZero BigDecimal employerInssRate,
        String legalBasis,
        @NotEmpty List<@Valid BracketRequest> brackets
) {
    public record BracketRequest(
            @NotNull @PositiveOrZero BigDecimal lowerBound,
            @PositiveOrZero BigDecimal upperBound,
            @NotNull @PositiveOrZero BigDecimal rate,
            @NotNull @PositiveOrZero BigDecimal fixedDeduction,
            @NotNull @PositiveOrZero BigDecimal dependentDeduction
    ) {}
}
