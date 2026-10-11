package mz.multicore.erp.modules.intelligence.service;

import java.math.BigDecimal;

/** Contexto mínimo e agregado autorizado a chegar ao motor de IA. */
public record ManagementAiContext(
        BigDecimal salesTotal30Days,
        long salesCount30Days,
        BigDecimal averageSale30Days,
        long lowStockPositions,
        long outOfStockPositions,
        long openPurchaseOrders,
        long overduePurchaseOrders,
        BigDecimal openPurchaseOrderValue
) {
}
