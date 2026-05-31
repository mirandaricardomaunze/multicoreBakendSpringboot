package com.phcpro.modules.pos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TillSessionDTO(
        Long id,
        String operator,
        Long companyId,
        BigDecimal openingBalance,
        BigDecimal closingBalanceExpected,
        BigDecimal closingBalanceReal,
        BigDecimal difference,
        LocalDateTime openDate,
        LocalDateTime closeDate,
        String status
) {}
