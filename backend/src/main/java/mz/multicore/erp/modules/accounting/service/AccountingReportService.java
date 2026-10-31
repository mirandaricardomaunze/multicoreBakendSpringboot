package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.modules.accounting.dto.BalanceSheetDTO;
import mz.multicore.erp.modules.accounting.dto.IncomeStatementDTO;
import mz.multicore.erp.modules.accounting.dto.LedgerDTO;
import mz.multicore.erp.modules.accounting.dto.TrialBalanceDTO;
import mz.multicore.erp.modules.accounting.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Razão, balancete, demonstração de resultados e balanço patrimonial.
 */
@Service
public class AccountingReportService {

    private final LedgerAndTrialBalanceCalculator ledgerCalculator;
    private final FinancialStatementBuilder statementBuilder;

    @Autowired
    public AccountingReportService(
            LedgerAndTrialBalanceCalculator ledgerCalculator,
            FinancialStatementBuilder statementBuilder
    ) {
        this.ledgerCalculator = ledgerCalculator;
        this.statementBuilder = statementBuilder;
    }

    public AccountingReportService(JournalService journalService, AccountRepository accountRepository) {
        LedgerAndTrialBalanceCalculator calc = new LedgerAndTrialBalanceCalculator(journalService, accountRepository);
        this.ledgerCalculator = calc;
        this.statementBuilder = new FinancialStatementBuilder(calc);
    }

    @Transactional(readOnly = true)
    public TrialBalanceDTO getTrialBalance(LocalDate from, LocalDate to) {
        return ledgerCalculator.getTrialBalance(from, to);
    }

    @Transactional(readOnly = true)
    public LedgerDTO getLedger(String accountCode, LocalDate from, LocalDate to) {
        return ledgerCalculator.getLedger(accountCode, from, to);
    }

    @Transactional(readOnly = true)
    public IncomeStatementDTO getIncomeStatement(LocalDate from, LocalDate to) {
        return statementBuilder.getIncomeStatement(from, to);
    }

    @Transactional(readOnly = true)
    public BalanceSheetDTO getBalanceSheet(LocalDate asOf) {
        return statementBuilder.getBalanceSheet(asOf);
    }
}
