package com.phcpro.modules.purchases.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchaseLineDTO(
        Long id,
        Long productId,
        String productName,
        String sku,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal taxRate,
        BigDecimal lineTotal,
        String batchNumber,
        LocalDate expirationDate,
        String serialNumber
) {}
