package com.phcpro.modules.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DailyStoreReportDTO(
        LocalDate date,
        SalesSummaryDTO sales,
        BigDecimal outstandingCredit,
        List<PaymentMethodSummaryDTO> paymentsByMethod,
        List<CashMovementSummaryDTO> cashMovements,
        List<TopProductDTO> topProducts,
        List<OperatorSalesSummaryDTO> salesByOperator,
        List<ProductMarginDTO> grossMarginByProduct
) {}
