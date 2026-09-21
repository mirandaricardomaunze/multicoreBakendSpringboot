package mz.multicore.erp.modules.accounting.dto;

import java.math.BigDecimal;

/** Linha agregada de uma demonstração financeira. */
public record FinancialStatementLineDTO(String accountCode, String accountName, BigDecimal amount) {}
