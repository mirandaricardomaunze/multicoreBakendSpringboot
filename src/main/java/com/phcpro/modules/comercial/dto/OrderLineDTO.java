package com.phcpro.modules.comercial.dto;

import java.math.BigDecimal;

public record OrderLineDTO(
    Long id,
    Long productId,
    String productName,
    String sku,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal taxRate,
    BigDecimal lineTotal,
    BigDecimal discountPercentage,
    String batchNumber,
    String serialNumber
) {}
