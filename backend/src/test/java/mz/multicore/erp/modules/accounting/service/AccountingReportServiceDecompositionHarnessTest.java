package mz.multicore.erp.modules.accounting.service;

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
        assertTrue(Files.readAllLines(DIR.resolve("AccountingReportService.java")).size() <= 65);
        assertTrue(Files.readAllLines(DIR.resolve("LedgerAndTrialBalanceCalculator.java")).size() <= 145);
        assertTrue(Files.readAllLines(DIR.resolve("FinancialStatementBuilder.java")).size() <= 90);
    }
}
