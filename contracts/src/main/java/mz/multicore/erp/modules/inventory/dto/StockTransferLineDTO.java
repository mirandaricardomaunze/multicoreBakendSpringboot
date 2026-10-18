package mz.multicore.erp.modules.inventory.dto;

import java.math.BigDecimal;

public record StockTransferLineDTO(
        Long id,
        Long productId,
        String productSku,
        String reference,
        String barcode,
        String productName,
        BigDecimal quantity,
        String batchNumber,
        Integer packagesPerBox,
        Integer unitsPerPackage,
        BigDecimal unitPrice,
        BigDecimal taxRate,
        /** Quantidade confirmada no destino; nulo enquanto a guia não foi recebida. */
        BigDecimal receivedQuantity,
        /** Quantidade devolvida à origem por divergência na recepção. */
        BigDecimal returnedQuantity,
        String discrepancyReason
) {
    /** Construtor retrocompatível de 12 argumentos (anterior à recepção com divergências). */
    public StockTransferLineDTO(
            Long id,
            Long productId,
            String productSku,
            String reference,
            String barcode,
            String productName,
            BigDecimal quantity,
            String batchNumber,
            Integer packagesPerBox,
            Integer unitsPerPackage,
            BigDecimal unitPrice,
            BigDecimal taxRate
    ) {
        this(id, productId, productSku, reference, barcode, productName, quantity, batchNumber,
                packagesPerBox, unitsPerPackage, unitPrice, taxRate, null, null, null);
    }

    /** Verdadeiro quando parte da linha voltou à origem na recepção. */
    public boolean hasDiscrepancy() {
        return returnedQuantity != null && returnedQuantity.signum() > 0;
    }

    public StockTransferLineDTO(
            Long id,
            Long productId,
            String productSku,
            String productName,
            BigDecimal quantity,
            String batchNumber
    ) {
        this(id, productId, productSku, productSku, null, productName, quantity, batchNumber, 1, 1, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public int safePackagesPerBox() {
        return packagesPerBox != null && packagesPerBox > 0 ? packagesPerBox : 1;
    }

    public int safeUnitsPerPackage() {
        return unitsPerPackage != null && unitsPerPackage > 0 ? unitsPerPackage : 1;
    }

    public BigDecimal safeUnitPrice() {
        return unitPrice != null ? unitPrice : BigDecimal.ZERO;
    }

    public BigDecimal safeTaxRate() {
        return taxRate != null ? taxRate : BigDecimal.ZERO;
    }
}
