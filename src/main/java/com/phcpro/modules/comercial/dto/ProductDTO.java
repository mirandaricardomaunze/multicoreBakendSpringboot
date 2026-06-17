package com.phcpro.modules.comercial.dto;

import java.math.BigDecimal;

public record ProductDTO(
    Long id,
    String sku,
    String reference,
    String barcode,
    String name,
    BigDecimal unitPrice,
    BigDecimal purchasePrice,
    BigDecimal minStock,
    int unitsPerBox,
    String saleType,
    boolean stockTracked,
    Long categoryId,
    String categoryName,
    String description
) {}
