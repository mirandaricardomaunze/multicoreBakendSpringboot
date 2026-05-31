package com.phcpro.modules.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateAbsenceRequest(
        @NotNull Long employeeId,
        @NotBlank String absenceType,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        String reason,
        boolean hasSupportingDocument
) {}
