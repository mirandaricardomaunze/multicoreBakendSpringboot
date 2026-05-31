package com.phcpro.modules.comercial.dto;

import java.math.BigDecimal;

public record CreditNoteLineDTO(
        Long id,
        Long productId,
        String productSku,
        String productName,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal taxRate,
        BigDecimal lineTotal,
        String batchNumber
) {}
