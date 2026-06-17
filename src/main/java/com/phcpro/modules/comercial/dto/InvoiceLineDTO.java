package com.phcpro.modules.comercial.dto;

import java.math.BigDecimal;

public record InvoiceLineDTO(
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
    String serialNumber
) {}
