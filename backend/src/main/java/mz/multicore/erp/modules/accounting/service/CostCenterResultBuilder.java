package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.accounting.dto.CostCenterResultDTO;
import mz.multicore.erp.modules.accounting.dto.CostCenterResultLineDTO;
import mz.multicore.erp.modules.accounting.model.CostCenter;
import mz.multicore.erp.modules.accounting.model.JournalEntry;
import mz.multicore.erp.modules.accounting.model.JournalLine;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds "Resultados por centro de custo" (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §3.3, CC-07) from the same posted
 * journal lines as the income statement: class 7 lines are revenue and class 6 lines are costs of their centre, each
 * signed by the account's nature exactly as the income statement does, so the "Total" always equals its net result.
 * Class 6/7 lines without a centre (posted before centres existed) fall into a "Sem centro" row instead of vanishing.
 */
@Component
public class CostCenterResultBuilder {

    static final String NO_CENTER_CODE = "—";
    static final String NO_CENTER_NAME = "Sem centro";

    private final JournalService journalService;

    public CostCenterResultBuilder(JournalService journalService) {
        this.journalService = journalService;
    }

    public CostCenterResultDTO getCostCenterResult(LocalDate from, LocalDate to) {
        FinancialStatementBuilder.validatePeriod(from, to);
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        Map<Long, CenterTotals> byCenter = new LinkedHashMap<>();
        for (JournalEntry entry : journalService.findEntriesBetween(companyId, from, to)) {
            for (JournalLine line : entry.getLines()) {
                String code = line.getAccount().getCode();
                boolean revenue = code.startsWith("7");
                if (!revenue && !code.startsWith("6")) continue;
                CostCenter center = line.getCostCenter();
                BigDecimal amount = line.getAccount().getNature().balanceOf(line.safeDebit(), line.safeCredit());
                byCenter.computeIfAbsent(center == null ? null : center.getId(), ignored -> new CenterTotals(center))
                        .add(revenue, amount);
            }
        }
        List<CostCenterResultLineDTO> lines = byCenter.values().stream().map(CenterTotals::toLine)
                .sorted(Comparator.comparing(CostCenterResultLineDTO::costCenterCode)).toList();
        BigDecimal totalRevenue = sum(lines.stream().map(CostCenterResultLineDTO::revenue).toList());
        BigDecimal totalExpense = sum(lines.stream().map(CostCenterResultLineDTO::expense).toList());
        return new CostCenterResultDTO(from, to, lines, totalRevenue, totalExpense,
                totalRevenue.subtract(totalExpense).setScale(2, RoundingMode.HALF_UP));
    }

    private static BigDecimal sum(List<BigDecimal> amounts) {
        return amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private static final class CenterTotals {
        private final CostCenter center;
        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal expense = BigDecimal.ZERO;

        private CenterTotals(CostCenter center) {
            this.center = center;
        }

        private void add(boolean isRevenue, BigDecimal amount) {
            if (isRevenue) revenue = revenue.add(amount);
            else expense = expense.add(amount);
        }

        private CostCenterResultLineDTO toLine() {
            BigDecimal rev = revenue.setScale(2, RoundingMode.HALF_UP);
            BigDecimal exp = expense.setScale(2, RoundingMode.HALF_UP);
            return new CostCenterResultLineDTO(
                    center == null ? null : center.getId(),
                    center == null ? NO_CENTER_CODE : center.getCode(),
                    center == null ? NO_CENTER_NAME : center.getName(),
                    rev, exp, rev.subtract(exp));
        }
    }
}
