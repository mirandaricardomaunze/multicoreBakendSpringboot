package mz.multicore.erp.modules.comercial.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Detalhe de uma factura em mora para efeitos de cobrança formal.
 */
public record OverdueInvoiceItemDTO(
        Long invoiceId,
        String invoiceNumber,
        LocalDate issueDate,
        LocalDate dueDate,
        int daysOverdue,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal outstandingAmount
) {}
