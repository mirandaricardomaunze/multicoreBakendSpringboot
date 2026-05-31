package com.phcpro.modules.reports.dto;

import java.math.BigDecimal;

public record LowStockAlertDTO(
        Long productId,
        String sku,
        String name,
        String warehouse,
        BigDecimal quantity,
        BigDecimal minStock
) {}
