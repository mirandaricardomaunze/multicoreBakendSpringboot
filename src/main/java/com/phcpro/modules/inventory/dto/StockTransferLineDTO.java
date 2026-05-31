package com.phcpro.modules.inventory.dto;

import java.math.BigDecimal;

public record StockTransferLineDTO(
        Long id,
        Long productId,
        String productSku,
        String productName,
        BigDecimal quantity,
        String batchNumber
) {}
