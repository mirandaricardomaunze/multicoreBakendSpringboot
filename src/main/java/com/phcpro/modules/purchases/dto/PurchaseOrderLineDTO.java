package com.phcpro.modules.purchases.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchaseOrderLineDTO(
        Long id,
        Long productId,
        String productName,
        String productSku,
        BigDecimal quantity,
        BigDecimal receivedQuantity,
        BigDecimal unitPrice,
        BigDecimal taxRate,
        BigDecimal lineTotal,
        String batchNumber,
        LocalDate expirationDate,
        String serialNumber
) {
    /** Quantidade ainda por receber (encomendada − recebida). */
    public BigDecimal outstandingQuantity() {
        BigDecimal q = quantity == null ? BigDecimal.ZERO : quantity;
        BigDecimal r = receivedQuantity == null ? BigDecimal.ZERO : receivedQuantity;
        return q.subtract(r);
    }
}
