package mz.multicore.erp.modules.hr.dto;

import java.time.LocalDateTime;

/** O mês da folha e o seu estado. Ver docs/RH_COMPLETO_SPEC.md §B8.6. */
public record PayrollPeriodDTO(
        Long id,
        int year,
        int month,
        String status,
        String statusLabel,
        String closedBy,
        LocalDateTime closedAt,
        String reopenReason
) {}
