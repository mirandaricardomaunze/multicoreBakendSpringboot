package mz.multicore.erp.modules.pickup.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** List row of a pickup account: the invoice whose material the client collects over time. */
public record PickupAccountSummaryDTO(
        Long id,
        Long invoiceId,
        String invoiceNumber,
        LocalDate invoiceDate,
        Long clientId,
        String clientName,
        String warehouseName,
        LocalDate deadline,
        boolean overdue,
        String status,
        String statusLabel,
        int lineCount,
        int pendingLineCount,
        LocalDateTime lastPickupAt
) {}
