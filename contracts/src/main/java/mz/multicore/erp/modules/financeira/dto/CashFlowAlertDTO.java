package mz.multicore.erp.modules.financeira.dto;

import java.math.BigDecimal;

public record CashFlowAlertDTO(
    String status,
    String alertMessage,
    String firstDeficitBucket,
    BigDecimal maxDeficitAmount,
    String recommendation
) {}
