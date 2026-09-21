package mz.multicore.erp.modules.financeira.dto;

import java.math.BigDecimal;

public record CashFlowBucketDTO(
    String bucketCode,
    String bucketLabel,
    BigDecimal inflows,
    BigDecimal outflows,
    BigDecimal netMovement,
    BigDecimal projectedCumulativeBalance
) {}
