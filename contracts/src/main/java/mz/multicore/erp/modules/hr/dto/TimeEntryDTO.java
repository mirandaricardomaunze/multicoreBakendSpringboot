package mz.multicore.erp.modules.hr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** Uma marcação. {@code workedHours} vem derivado da entidade — não é coluna. */
public record TimeEntryDTO(
        Long id,
        Long employeeId,
        String employeeName,
        LocalDate entryDate,
        LocalTime checkIn,
        LocalTime checkOut,
        int breakMinutes,
        BigDecimal workedHours,
        boolean crossesMidnight,
        String source,
        String sourceLabel,
        String recordedBy,
        String notes
) {}
