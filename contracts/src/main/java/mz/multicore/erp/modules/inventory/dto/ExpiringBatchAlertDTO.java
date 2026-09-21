package mz.multicore.erp.modules.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpiringBatchAlertDTO(
        Long batchId,
        String batchNumber,
        Long productId,
        String productSku,
        String productName,
        String categoryName,
        Long warehouseId,
        String warehouseName,
        BigDecimal quantity,
        BigDecimal unitCost,
        BigDecimal potentialLossValue,
        LocalDate expirationDate,
        long daysUntilExpiration,
        String alertLevel
) {}
