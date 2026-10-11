package mz.multicore.erp.modules.comercial.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Recibo de liquidação de fatura (vista de tabela do painel comercial). */
public record ReceiptDTO(
        Long id,
        String receiptNumber,
        String invoiceNumber,
        String clientName,
        BigDecimal amountPaid,
        String paymentMethod,
        String status,
        LocalDateTime receiptDate,
        Long businessUnitId,
        String businessUnitName
) {
    public ReceiptDTO(
            Long id,
            String receiptNumber,
            String invoiceNumber,
            String clientName,
            BigDecimal amountPaid,
            String paymentMethod,
            String status,
            LocalDateTime receiptDate
    ) {
        this(id, receiptNumber, invoiceNumber, clientName, amountPaid, paymentMethod, status, receiptDate, null, null);
    }
}

