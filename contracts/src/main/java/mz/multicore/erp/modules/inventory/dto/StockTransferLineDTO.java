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
        BigDecimal taxRate
) {
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
