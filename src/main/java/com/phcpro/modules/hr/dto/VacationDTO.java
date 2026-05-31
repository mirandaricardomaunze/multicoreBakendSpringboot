package com.phcpro.modules.hr.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record VacationDTO(
        Long id,
        Long employeeId,
        String employeeName,
        LocalDate startDate,
        LocalDate endDate,
        int totalDays,
        int yearReference,
        String status,
        String notes,
        String decisionBy,
        LocalDateTime decisionAt,
        String rejectionReason
) {}
