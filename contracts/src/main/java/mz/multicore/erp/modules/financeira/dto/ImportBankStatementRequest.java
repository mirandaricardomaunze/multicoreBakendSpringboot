package mz.multicore.erp.modules.financeira.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request to import and record a bank statement for treasury review.
 */
public record ImportBankStatementRequest(
        @NotNull(message = "A conta de tesouraria é obrigatória.")
        Long treasuryAccountId,

        @Size(max = 100, message = "A referência do extracto não pode exceder 100 caracteres.")
        String statementReference,

        LocalDate startDate,
        LocalDate endDate,
        BigDecimal openingBalance,
        BigDecimal closingBalance,

        @Size(max = 20000, message = "O extracto não pode ter mais de 20000 linhas por importação.")
        List<BankStatementItemImportDTO> items
) {}
