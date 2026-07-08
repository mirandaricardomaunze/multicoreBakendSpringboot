package com.phcpro.modules.inventory.dto;

import java.math.BigDecimal;

/** Produto em ruptura de stock (esgotado): controla stock e tem saldo total ≤ 0 na empresa. */
public record StockAlertDTO(
        Long productId,
        String sku,
        String name,
        BigDecimal currentStock
) {}
