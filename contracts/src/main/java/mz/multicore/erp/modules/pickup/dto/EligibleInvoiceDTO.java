package mz.multicore.erp.modules.pickup.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Realised invoice that can still become a pickup account. */
public record EligibleInvoiceDTO(
        Long invoiceId,
        String invoiceNumber,
        LocalDate invoiceDate,
        String clientName,
        BigDecimal totalAmount
) {}
