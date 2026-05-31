package com.phcpro.modules.hr.dto;

import java.time.LocalDate;

public record AbsenceDTO(
        Long id,
        Long employeeId,
        String employeeName,
        String absenceType,
        LocalDate startDate,
        LocalDate endDate,
        int totalDays,
        String reason,
        boolean hasSupportingDocument
) {}
