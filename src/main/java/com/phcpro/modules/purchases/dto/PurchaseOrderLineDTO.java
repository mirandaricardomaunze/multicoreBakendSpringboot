package com.phcpro.modules.purchases.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchaseOrderLineDTO(
        Long id,
        Long productId,
        String productName,
        String productSku,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal taxRate,
        BigDecimal lineTotal,
        String batchNumber,
        LocalDate expirationDate,
        String serialNumber
) {}
