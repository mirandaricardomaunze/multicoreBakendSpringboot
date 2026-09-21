package mz.multicore.erp.modules.inventory.dto;

import mz.multicore.erp.modules.inventory.model.WasteReason;

import java.math.BigDecimal;
import java.util.Map;

public record WasteSummaryDTO(
        BigDecimal totalWasteCost,
        BigDecimal totalWasteQuantity,
        long totalRecordsCount,
        BigDecimal totalRevenueInPeriod,
        BigDecimal wasteRatePercentage,
        Map<WasteReason, BigDecimal> costByReason,
        Map<String, BigDecimal> costByCategory
) {}
