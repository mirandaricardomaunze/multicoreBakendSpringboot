package mz.multicore.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BalanceSheetDTO(
        LocalDate asOf,
        List<FinancialStatementLineDTO> assets,
        List<FinancialStatementLineDTO> liabilities,
        List<FinancialStatementLineDTO> equity,
        BigDecimal currentResult,
        BigDecimal totalAssets,
        BigDecimal totalLiabilities,
        BigDecimal totalEquityAndLiabilities,
        boolean balanced
) {}
