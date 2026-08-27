package mz.multicore.erp.modules.reports.dto;

import java.math.BigDecimal;

public record OperatorSalesSummaryDTO(
        String operator,
        long salesCount,
        BigDecimal total
) {}
