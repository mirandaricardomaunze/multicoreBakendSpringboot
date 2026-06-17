package com.phcpro.modules.reports.dto;

import java.math.BigDecimal;

public record PaymentMethodSummaryDTO(
        String method,
        BigDecimal amount
) {}
