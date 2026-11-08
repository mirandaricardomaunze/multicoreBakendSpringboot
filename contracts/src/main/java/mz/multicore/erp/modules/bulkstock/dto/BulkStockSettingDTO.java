package mz.multicore.erp.modules.bulkstock.dto;

import java.math.BigDecimal;

/** A product measured in bulk and the natural-loss tolerance applied without approval. */
public record BulkStockSettingDTO(
        Long productId,
        String productSku,
        String productName,
        String baseUnitCode,
        BigDecimal tolerancePercent
) {}
