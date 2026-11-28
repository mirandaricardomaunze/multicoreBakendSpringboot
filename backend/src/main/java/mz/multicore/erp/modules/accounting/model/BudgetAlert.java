package mz.multicore.erp.modules.accounting.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;

import java.math.BigDecimal;

/**
 * Budget alert (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4.3): the actual costs (class 6) of a cost centre in a month
 * reached a threshold (90% or 100%) of its budgeted costs. One per company × centre × year × month × threshold.
 */
@Entity
@Table(name = "budget_alerts", uniqueConstraints = @UniqueConstraint(
        name = "uk_budget_alerts_threshold",
        columnNames = {"company_id", "cost_center_id", "fiscal_year", "alert_month", "threshold_percent"}))
@Getter
@Setter
public class BudgetAlert extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id", nullable = false)
    private CostCenter costCenter;

    @Column(name = "fiscal_year", nullable = false)
    private int fiscalYear;

    @Column(name = "alert_month", nullable = false)
    private int month;

    @Column(name = "threshold_percent", nullable = false)
    private int thresholdPercent;

    /** Budgeted costs of the centre in the month when the alert was raised. */
    @Column(name = "budgeted_amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal budgetedAmount;

    /** Actual costs of the centre in the month when the alert was raised. */
    @Column(name = "actual_amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal actualAmount;
}
