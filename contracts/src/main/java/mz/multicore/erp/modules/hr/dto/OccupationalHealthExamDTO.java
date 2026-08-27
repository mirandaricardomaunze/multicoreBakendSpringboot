package mz.multicore.erp.modules.hr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OccupationalHealthExamDTO(
        Long id, Long employeeId, String employeeName, String cardNumber,
        LocalDate examDate, LocalDate expiryDate, String fitnessResult,
        Long providerId, String providerName, String clinic, String doctorName,
        String restrictions, String notes,
        BigDecimal cost, String invoiceNumber, boolean paid, LocalDate paidAt,
        boolean hasAttachment, String attachmentName, long daysUntilExpiry, String validityStatus
) {}
