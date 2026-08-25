package mz.multicore.erp.modules.hr.dto;

import java.time.LocalDate;

public record OccupationalHealthExamDTO(
        Long id, Long employeeId, String employeeName, String cardNumber,
        LocalDate examDate, LocalDate expiryDate, String fitnessResult,
        String clinic, String doctorName, String restrictions, String notes,
        boolean hasAttachment, String attachmentName, long daysUntilExpiry, String validityStatus
) {}
