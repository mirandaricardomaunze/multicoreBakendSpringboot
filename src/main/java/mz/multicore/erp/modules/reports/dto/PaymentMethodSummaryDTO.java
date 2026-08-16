package mz.multicore.erp.modules.reports.dto;

import java.math.BigDecimal;

public record PaymentMethodSummaryDTO(
        String method,
        BigDecimal amount
) {}
