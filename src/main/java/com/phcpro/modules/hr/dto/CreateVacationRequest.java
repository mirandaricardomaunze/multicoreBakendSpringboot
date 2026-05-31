package com.phcpro.modules.hr.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateVacationRequest(
        @NotNull Long employeeId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull @Min(2000) @Max(2100) Integer yearReference,
        String notes
) {}
