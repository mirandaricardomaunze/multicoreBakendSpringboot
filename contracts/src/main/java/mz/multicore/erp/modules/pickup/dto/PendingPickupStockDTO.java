package mz.multicore.erp.modules.pickup.dto;

import java.math.BigDecimal;

/**
 * Material already sold (and already out of the stock balance) that is still physically in the yard.
 * Explains the difference a physical count finds against the system stock.
 */
public record PendingPickupStockDTO(
        Long productId,
        String productSku,
        String productName,
        String unitCode,
        String warehouseName,
        BigDecimal pendingQuantity,
        int accountCount
) {}
