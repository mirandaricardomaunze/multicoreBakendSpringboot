package com.phcpro.modules.inventory.dto;

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
        BigDecimal minStock
) {}
