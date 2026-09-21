package mz.multicore.erp.modules.performance.dto;

import java.math.BigDecimal;

public record AdjustBonusRequest(
        BigDecimal adjustedAmount,
        String justification
) {}
