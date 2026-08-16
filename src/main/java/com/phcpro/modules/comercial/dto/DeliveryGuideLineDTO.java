package com.phcpro.modules.comercial.dto;

import java.math.BigDecimal;

public record DeliveryGuideLineDTO(
        Long id,
        Long productId,
        String productSku,
        String productName,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        String batchNumber
) {}
