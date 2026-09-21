package mz.multicore.erp.modules.comercial.dto;

import java.math.BigDecimal;

public record POSSalesSummaryDTO(
        long count,
        BigDecimal totalAmount,
        Long previousCount,
        BigDecimal previousTotalAmount,
        BigDecimal countVariationPercent,
        BigDecimal totalVariationPercent
) {}
