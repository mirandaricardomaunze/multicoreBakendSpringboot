package mz.multicore.erp.modules.financeira.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Linha de movimento a importar a partir de um ficheiro de extracto bancário.
 */
public record BankStatementItemImportDTO(
        LocalDate transactionDate,
        LocalDate valueDate,
        String description,
        String reference,
        BigDecimal amount,
        BigDecimal balanceAfter
) {}
