package mz.multicore.erp.modules.hr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OvertimeRateConfigDTO(
        Long id,
        String name,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        BigDecimal dayMultiplier,
        BigDecimal nightMultiplier,
        BigDecimal restDayMultiplier,
        String legalBasis,
        boolean active
) {}
