package mz.multicore.erp.modules.reports.dto;

import java.math.BigDecimal;

public record SalesSummaryDTO(long count, BigDecimal totalAmount) {}
