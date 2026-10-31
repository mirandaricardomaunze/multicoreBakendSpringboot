package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.accounting.dto.BalanceSheetDTO;
import mz.multicore.erp.modules.accounting.dto.FinancialStatementLineDTO;
import mz.multicore.erp.modules.accounting.dto.IncomeStatementDTO;
import mz.multicore.erp.modules.accounting.model.AccountNature;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class FinancialStatementBuilder {

    private final LedgerAndTrialBalanceCalculator ledgerCalculator;

    public FinancialStatementBuilder(LedgerAndTrialBalanceCalculator ledgerCalculator) {
        this.ledgerCalculator = ledgerCalculator;
    }

    public IncomeStatementDTO getIncomeStatement(LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        Map<String, LedgerAndTrialBalanceCalculator.Totals> totals = ledgerCalculator.totalsBetween(from, to);
        List<FinancialStatementLineDTO> revenues = statementLines(totals, "7");
        List<FinancialStatementLineDTO> expenses = statementLines(totals, "6");
        BigDecimal totalRevenue = sum(revenues);
        BigDecimal totalExpense = sum(expenses);
        return new IncomeStatementDTO(from, to, revenues, expenses, totalRevenue, totalExpense,
                totalRevenue.subtract(totalExpense).setScale(2, RoundingMode.HALF_UP));
    }

    public BalanceSheetDTO getBalanceSheet(LocalDate asOf) {
        if (asOf == null) throw new BusinessRuleException("A data do balanço é obrigatória.");
        Map<String, LedgerAndTrialBalanceCalculator.Totals> totals = ledgerCalculator.totalsBetween(LocalDate.of(1970, 1, 1), asOf);
        List<FinancialStatementLineDTO> assets = totals.values().stream()
                .filter(t -> t.account().getNature() == AccountNature.DEVEDORA)
                .filter(t -> "1234".contains(t.account().getCode().substring(0, 1)))
                .map(LedgerAndTrialBalanceCalculator.Totals::toStatementLine).filter(l -> l.amount().signum() != 0)
                .sorted(Comparator.comparing(FinancialStatementLineDTO::accountCode)).toList();
        List<FinancialStatementLineDTO> liabilities = totals.values().stream()
                .filter(t -> t.account().getNature() == AccountNature.CREDORA)
                .filter(t -> t.account().getCode().startsWith("2"))
                .map(LedgerAndTrialBalanceCalculator.Totals::toStatementLine).filter(l -> l.amount().signum() != 0)
                .sorted(Comparator.comparing(FinancialStatementLineDTO::accountCode)).toList();
        List<FinancialStatementLineDTO> equity = statementLines(totals, "5");
        BigDecimal currentResult = sum(statementLines(totals, "7"))
                .subtract(sum(statementLines(totals, "6"))).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAssets = sum(assets);
        BigDecimal totalLiabilities = sum(liabilities);
        BigDecimal totalEquityAndLiabilities = totalLiabilities.add(sum(equity)).add(currentResult).setScale(2, RoundingMode.HALF_UP);
        return new BalanceSheetDTO(asOf, assets, liabilities, equity, currentResult, totalAssets,
                totalLiabilities, totalEquityAndLiabilities, totalAssets.compareTo(totalEquityAndLiabilities) == 0);
    }

    private List<FinancialStatementLineDTO> statementLines(Map<String, LedgerAndTrialBalanceCalculator.Totals> totals, String accountPrefix) {
        return totals.values().stream().filter(t -> t.account().getCode().startsWith(accountPrefix))
                .map(LedgerAndTrialBalanceCalculator.Totals::toStatementLine).filter(line -> line.amount().signum() != 0)
                .sorted(Comparator.comparing(FinancialStatementLineDTO::accountCode)).toList();
    }

    private BigDecimal sum(List<FinancialStatementLineDTO> lines) {
        return lines.stream().map(FinancialStatementLineDTO::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private void validatePeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null) throw new BusinessRuleException("O período é obrigatório.");
        if (to.isBefore(from)) throw new BusinessRuleException("A data final não pode ser anterior à inicial.");
    }
}
