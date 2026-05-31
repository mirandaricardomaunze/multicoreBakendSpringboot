package com.phcpro.modules.hr.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreatePayslipRequest(
        @NotNull Long employeeId,
        @NotNull @Min(2000) @Max(2100) Integer year,
        @NotNull @Min(1) @Max(12) Integer month,
        @PositiveOrZero BigDecimal allowances,
        @PositiveOrZero BigDecimal overtime,
        @PositiveOrZero BigDecimal irpsDeduction,
        @PositiveOrZero BigDecimal inssDeduction,
        @PositiveOrZero BigDecimal otherDeductions,
        String notes
) {}
