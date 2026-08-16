package mz.multicore.erp.modules.comercial.dto;

import java.math.BigDecimal;

public record OrderLineDTO(
    Long id,
    Long productId,
    String productName,
    String sku,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal taxRate,
    BigDecimal lineTotal,
    BigDecimal discountPercentage,
    String batchNumber,
    String serialNumber,
    int unitsPerBox,
    BigDecimal netUnitWeightKg,
    BigDecimal grossUnitWeightKg,
    BigDecimal lineGrossWeightKg,
    BigDecimal quantityPercentage,
    BigDecimal weightPercentage
) {}
