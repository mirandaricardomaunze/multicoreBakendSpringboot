package mz.multicore.erp.modules.accounting.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.accounting.dto.BudgetVarianceDTO;
import mz.multicore.erp.modules.accounting.dto.BudgetVarianceLineDTO;
import mz.multicore.erp.modules.accounting.dto.BudgetVarianceQuery;
import mz.multicore.erp.modules.accounting.model.Account;
import mz.multicore.erp.modules.accounting.model.AccountClass;
import mz.multicore.erp.modules.accounting.model.Budget;
import mz.multicore.erp.modules.accounting.model.BudgetVarianceDirection;
import mz.multicore.erp.modules.accounting.model.CostCenter;
import mz.multicore.erp.modules.accounting.repository.BudgetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Real vs orçado of the active company (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4.2, OR-03): one row per account ×
 * cost centre with the budgeted and the actual amount of a month, or of January up to that month when cumulative.
 * Rows come from the budget cells and from the actual cells alike; the missing side counts as zero. Each row carries
 * its variance, variance % and favourable/unfavourable sense by class (OR-04).
 */
@Service
public class BudgetVarianceService {

    private static final Comparator<Row> ROW_ORDER = Comparator
            .comparing((Row row) -> row.account.getCode())
            .thenComparing(row -> row.center.getCode());

    private final BudgetRepository budgetRepository;
    private final BudgetActualsCalculator actualsCalculator;

    public BudgetVarianceService(BudgetRepository budgetRepository, BudgetActualsCalculator actualsCalculator) {
        this.budgetRepository = budgetRepository;
        this.actualsCalculator = actualsCalculator;
    }

    @Transactional(readOnly = true)
    public BudgetVarianceDTO report(BudgetVarianceQuery query) {
        PermissionGuard.requireManagerOrAdmin("consultar o real vs orçado");
        if (query.year() == null) {
            throw new BusinessRuleException("Indique o ano do relatório real vs orçado.");
        }
        if (query.month() == null || query.month() < 1 || query.month() > 12) {
            throw new BusinessRuleException("O mês do relatório real vs orçado tem de estar entre 1 e 12.");
        }
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        Period period = new Period(query.month(), query.cumulative(), query.costCenterId());

        Map<String, Row> rows = new LinkedHashMap<>();
        budgetRepository.findByCompanyIdAndFiscalYear(companyId, query.year())
                .map(Budget::getLines).orElse(List.of()).stream()
                .filter(cell -> period.includes(cell.getMonth(), cell.getCostCenter()))
                .forEach(cell -> rowOf(rows, cell.getAccount(), cell.getCostCenter()).addBudgeted(cell.getAmount()));
        actualsCalculator.actualsOf(companyId, query.year()).stream()
                .filter(cell -> period.includes(cell.month(), cell.costCenter()))
                .forEach(cell -> rowOf(rows, cell.account(), cell.costCenter()).addActual(cell.amount()));

        List<BudgetVarianceLineDTO> lines = rows.values().stream().sorted(ROW_ORDER).map(Row::toDTO).toList();
        return new BudgetVarianceDTO(query.year(), query.month(), query.cumulative(), query.costCenterId(), lines);
    }

    private static Row rowOf(Map<String, Row> rows, Account account, CostCenter center) {
        return rows.computeIfAbsent(account.getCode() + "|" + center.getId(), key -> new Row(account, center));
    }

    /** Months and centre a report covers. */
    private record Period(int month, boolean cumulative, Long costCenterId) {
        boolean includes(int cellMonth, CostCenter center) {
            boolean inMonths = cumulative ? cellMonth <= month : cellMonth == month;
            return inMonths && (costCenterId == null || Objects.equals(costCenterId, center.getId()));
        }
    }

    /** Running totals of one account × centre. */
    private static final class Row {
        private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

        private final Account account;
        private final CostCenter center;
        private BigDecimal budgeted = BigDecimal.ZERO;
        private BigDecimal actual = BigDecimal.ZERO;

        private Row(Account account, CostCenter center) {
            this.account = account;
            this.center = center;
        }

        void addBudgeted(BigDecimal amount) {
            budgeted = budgeted.add(amount);
        }

        void addActual(BigDecimal amount) {
            actual = actual.add(amount);
        }

        BudgetVarianceLineDTO toDTO() {
            return new BudgetVarianceLineDTO(account.getCode(), account.getName(),
                    account.getAccountClass().digit(), center.getId(), center.getCode(), center.getName(),
                    budgeted, actual, variance(), variancePercent(), direction());
        }

        private BigDecimal variance() {
            return actual.subtract(budgeted);
        }

        /** Variance over the budget in %, 2 decimals half-up; empty when nothing was budgeted. */
        private BigDecimal variancePercent() {
            if (budgeted.signum() == 0) return null;
            return variance().multiply(HUNDRED).divide(budgeted, 2, RoundingMode.HALF_UP);
        }

        /** A cost above budget or a revenue below budget is unfavourable. */
        private BudgetVarianceDirection direction() {
            int sign = variance().signum();
            if (sign == 0) return BudgetVarianceDirection.ON_BUDGET;
            boolean overBudget = sign > 0;
            boolean cost = account.getAccountClass() == AccountClass.CUSTOS_E_PERDAS;
            return overBudget == cost ? BudgetVarianceDirection.UNFAVORABLE : BudgetVarianceDirection.FAVORABLE;
        }
    }
}
