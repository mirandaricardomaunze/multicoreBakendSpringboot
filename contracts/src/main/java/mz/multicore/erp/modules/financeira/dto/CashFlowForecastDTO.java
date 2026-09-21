package mz.multicore.erp.modules.financeira.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CashFlowForecastDTO(
    LocalDate generatedDate,
    BigDecimal currentCashBalance,
    BigDecimal currentBankBalance,
    BigDecimal totalAvailableLiquidity,
    BigDecimal totalReceivables,
    BigDecimal totalPayables,
    BigDecimal netProjectedPosition,
    List<CashFlowBucketDTO> buckets,
    List<CashFlowItemDTO> topReceivables,
    List<CashFlowItemDTO> topPayables,
    CashFlowAlertDTO alert
) {}
