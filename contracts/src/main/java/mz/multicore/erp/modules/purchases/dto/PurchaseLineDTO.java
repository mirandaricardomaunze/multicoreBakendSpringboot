package mz.multicore.erp.modules.purchases.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchaseLineDTO(
        Long id,
        Long productId,
        String productName,
        String sku,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal taxRate,
        BigDecimal lineTotal,
        String batchNumber,
        LocalDate expirationDate,
        String serialNumber,
        /** Quantidade já devolvida ao fornecedor. */
        BigDecimal returnedQuantity
) {
    /** Construtor retrocompatível, anterior às devoluções ao fornecedor. */
    public PurchaseLineDTO(Long id, Long productId, String productName, String sku, BigDecimal quantity,
                           BigDecimal unitPrice, BigDecimal taxRate, BigDecimal lineTotal, String batchNumber,
                           LocalDate expirationDate, String serialNumber) {
        this(id, productId, productName, sku, quantity, unitPrice, taxRate, lineTotal, batchNumber, expirationDate,
                serialNumber, BigDecimal.ZERO);
    }
}
