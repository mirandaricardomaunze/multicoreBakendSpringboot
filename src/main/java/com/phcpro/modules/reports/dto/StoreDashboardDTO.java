package com.phcpro.modules.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record StoreDashboardDTO(
        LocalDate date,
        SalesSummaryDTO salesToday,
        List<TopProductDTO> topProductsToday,
        List<LowStockAlertDTO> lowStockAlerts,
        long pendingApprovals,
        BigDecimal unpaidInvoicesAmount,
        List<OpenTillSessionDTO> openTillSessions
) {}
