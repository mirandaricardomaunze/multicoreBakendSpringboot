package mz.multicore.erp.modules.comercial.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OrderPickingLineDTO(
    Long lineId,
    Long productId,
    String productReference,
    String barcode,
    String productName,
    BigDecimal requestedQuantity,
    BigDecimal confirmedQuantity,
    Integer packagesPerBox,
    Integer unitsPerPackage,
    BigDecimal boxes,
    String batchNumber,
    LocalDate expiryDate,
    String warehouseLocation
) {
    public OrderPickingLineDTO withConfirmedQuantity(BigDecimal newConfirmed) {
        return new OrderPickingLineDTO(
            lineId, productId, productReference, barcode, productName,
            requestedQuantity, newConfirmed, packagesPerBox, unitsPerPackage,
            boxes, batchNumber, expiryDate, warehouseLocation
        );
    }
}
