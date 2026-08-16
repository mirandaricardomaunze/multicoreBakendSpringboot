package com.phcpro.modules.comercial.dto;

import com.phcpro.modules.comercial.model.AgingBucket;
import com.phcpro.modules.comercial.model.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @param dueDate      data-limite de pagamento
 * @param daysOverdue  dias de atraso à data do servidor (0 = dentro do prazo ou nada a receber)
 * @param agingBucket  escalão de antiguidade calculado no servidor, para a UI não repetir a regra
 */
public record InvoiceDTO(
    Long id,
    String invoiceNumber,
    Long clientId,
    String clientName,
    String clientTaxId,
    BigDecimal totalBeforeTax,
    BigDecimal taxAmount,
    BigDecimal totalAmount,
    BigDecimal amountPaid,
    InvoiceStatus status,
    String rejectionReason,
    List<InvoiceLineDTO> lines,
    LocalDateTime createdAt,
    String createdBy,
    LocalDate dueDate,
    int daysOverdue,
    AgingBucket agingBucket
) {

    /** Saldo por liquidar — espelha {@code Invoice.outstandingAmount()} para a UI não o recalcular. */
    public BigDecimal outstandingAmount() {
        BigDecimal total = totalAmount == null ? BigDecimal.ZERO : totalAmount;
        BigDecimal paid = amountPaid == null ? BigDecimal.ZERO : amountPaid;
        BigDecimal remaining = total.subtract(paid);
        return remaining.signum() > 0 ? remaining : BigDecimal.ZERO;
    }
}
