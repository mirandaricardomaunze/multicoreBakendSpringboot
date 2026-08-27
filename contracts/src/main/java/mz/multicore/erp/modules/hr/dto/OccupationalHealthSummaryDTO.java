package mz.multicore.erp.modules.hr.dto;

import java.time.LocalDate;

/** Resumo sem informação clínica detalhada, seguro para a ficha geral do trabalhador. */
public record OccupationalHealthSummaryDTO(
        Long employeeId, boolean hasExam, String fitnessResult,
        LocalDate examDate, LocalDate expiryDate, Long daysUntilExpiry, String validityStatus
) {}
