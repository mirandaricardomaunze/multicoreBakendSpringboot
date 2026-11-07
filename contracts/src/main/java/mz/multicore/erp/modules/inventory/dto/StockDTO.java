package mz.multicore.erp.modules.inventory.dto;

import java.math.BigDecimal;

public record StockDTO(
        Long id,
        Long productId,
        String sku,
        String reference,
        String barcode,
        String productName,
        Long warehouseId,
        String warehouseName,
        BigDecimal quantity,
        BigDecimal minStock,
        String categoryName,
        BigDecimal unitPrice,
        int unitsPerBox,
        Long businessUnitId,
        String businessUnitName,
        /** Produto perecível: entradas exigem validade. Ver docs/PRODUTO_PERECIVEL_VALIDADE_SPEC.md. */
        boolean tracksExpiry
) {
    /** Compatível com chamadas anteriores à opção "perecível". */
    public StockDTO(Long id, Long productId, String sku, String reference, String barcode,
                    String productName, Long warehouseId, String warehouseName,
                    BigDecimal quantity, BigDecimal minStock, String categoryName,
                    BigDecimal unitPrice, int unitsPerBox, Long businessUnitId, String businessUnitName) {
        this(id, productId, sku, reference, barcode, productName, warehouseId, warehouseName,
                quantity, minStock, categoryName, unitPrice, unitsPerBox, businessUnitId, businessUnitName, false);
    }

    public StockDTO(Long id, Long productId, String sku, String reference, String barcode,
                    String productName, Long warehouseId, String warehouseName,
                    BigDecimal quantity, BigDecimal minStock, String categoryName,
                    BigDecimal unitPrice, int unitsPerBox) {
        this(id, productId, sku, reference, barcode, productName, warehouseId, warehouseName,
                quantity, minStock, categoryName, unitPrice, unitsPerBox, null, null);
    }
}
