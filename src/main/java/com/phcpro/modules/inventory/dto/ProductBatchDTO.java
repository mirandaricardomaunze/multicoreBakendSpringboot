package com.phcpro.modules.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductBatchDTO(
        Long id,
        Long productId,
        String sku,
        String productName,
        Long warehouseId,
        String warehouseName,
        String batchNumber,
        LocalDate expirationDate,
        LocalDate entryDate,
        BigDecimal quantity
) {}
