package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.SourceLines;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountingReportServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/accounting/service");

    @Test
    void ledgerAndFinancialStatementsStayExtractedFromFacade() throws Exception {
        String facade = Files.readString(DIR.resolve("AccountingReportService.java"));
        String ledger = Files.readString(DIR.resolve("LedgerAndTrialBalanceCalculator.java"));
        String statements = Files.readString(DIR.resolve("FinancialStatementBuilder.java"));

        assertTrue(facade.contains("ledgerCalculator.getTrialBalance(from, to)"));
        assertTrue(facade.contains("statementBuilder.getBalanceSheet(asOf)"));
        assertFalse(facade.contains("AccountNature.DEVEDORA"));
        assertTrue(ledger.contains("totalDebit.compareTo(totalCredit) == 0"));
        assertTrue(statements.contains("totalAssets.compareTo(totalEquityAndLiabilities) == 0"));
        assertTrue(SourceLines.code(DIR.resolve("AccountingReportService.java")) <= 65);
        assertTrue(SourceLines.code(DIR.resolve("LedgerAndTrialBalanceCalculator.java")) <= 145);
        assertTrue(SourceLines.code(DIR.resolve("FinancialStatementBuilder.java")) <= 90);
    }
}
