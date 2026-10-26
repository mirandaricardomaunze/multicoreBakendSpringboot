package mz.multicore.erp.modules.comercial.dto;

import mz.multicore.erp.modules.comercial.model.CollectionRadarBucket;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Item individual no Radar de Cobranças representando uma fatura em aberto com cálculo de horizonte temporal.
 */
public record CollectionRadarItemDTO(
        Long invoiceId,
        String invoiceNumber,
        Long clientId,
        String clientName,
        String clientNuit,
        String clientPhone,
        String clientEmail,
        LocalDate issueDate,
        LocalDate dueDate,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal remainingBalance,
        int daysUntilDue,
        CollectionRadarBucket bucket
) {
    public CollectionRadarItemDTO {
        if (totalAmount == null) totalAmount = BigDecimal.ZERO;
        if (paidAmount == null) paidAmount = BigDecimal.ZERO;
        if (remainingBalance == null) remainingBalance = totalAmount.subtract(paidAmount);
    }
}
