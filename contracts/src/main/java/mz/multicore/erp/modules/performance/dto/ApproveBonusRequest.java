package mz.multicore.erp.modules.performance.dto;

import java.math.BigDecimal;

public record ApproveBonusRequest(
        BigDecimal approvedAmount,
        String justification
) {}
