package com.phcpro.modules.reports.dto;

import java.math.BigDecimal;

public record ProductMarginDTO(
        Long productId,
        String sku,
        String name,
        BigDecimal revenue,
        BigDecimal estimatedCost,
        BigDecimal grossMargin
) {}
