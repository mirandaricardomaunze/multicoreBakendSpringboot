package mz.multicore.erp.modules.accounting.dto;

import java.math.BigDecimal;

/** Aggregated line in a financial statement. */
public record FinancialStatementLineDTO(String accountCode, String accountName, BigDecimal amount) {}
