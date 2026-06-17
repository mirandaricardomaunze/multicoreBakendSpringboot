package com.phcpro.modules.reports.dto;

import java.math.BigDecimal;

public record TopProductDTO(
        Long productId,
        String sku,
        String name,
        BigDecimal quantitySold,
        BigDecimal revenue
) {}
