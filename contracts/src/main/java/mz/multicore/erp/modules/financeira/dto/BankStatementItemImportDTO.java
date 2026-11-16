package mz.multicore.erp.modules.financeira.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Transaction line imported from a bank-statement file.
 */
public record BankStatementItemImportDTO(
        LocalDate transactionDate,
        LocalDate valueDate,
        String description,
        String reference,
        BigDecimal amount,
        BigDecimal balanceAfter
) {}
