package mz.multicore.erp.modules.financeira.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Pedido de importação e registo de um extracto bancário para conferência de tesouraria.
 */
public record ImportBankStatementRequest(
        Long treasuryAccountId,
        String statementReference,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal openingBalance,
        BigDecimal closingBalance,
        List<BankStatementItemImportDTO> items
) {}
