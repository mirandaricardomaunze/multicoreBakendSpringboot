package mz.multicore.erp.modules.advances.dto;

import mz.multicore.erp.modules.advances.model.CustomerAdvanceStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** An advance with the documents of its whole circuit (advance invoice, receipt, credit note, offset). */
public record CustomerAdvanceDTO(
        Long id,
        Long clientId,
        String clientName,
        Long orderId,
        String orderNumber,
        BigDecimal amount,
        CustomerAdvanceStatus status,
        Long advanceInvoiceId,
        String advanceInvoiceNumber,
        Long receiptId,
        String receiptNumber,
        String paymentMethod,
        Long appliedInvoiceId,
        String appliedInvoiceNumber,
        Long creditNoteId,
        String creditNoteNumber,
        String offsetReceiptNumber,
        String notes,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime appliedAt
) {}
