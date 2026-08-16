package mz.multicore.erp.modules.reports.dto;

import java.math.BigDecimal;

public record CashMovementSummaryDTO(
        String movementType,
        BigDecimal amount
) {}
