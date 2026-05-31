package com.phcpro.modules.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OpenTillSessionDTO(
        Long sessionId,
        String operator,
        BigDecimal openingBalance,
        LocalDateTime openedAt
) {}
