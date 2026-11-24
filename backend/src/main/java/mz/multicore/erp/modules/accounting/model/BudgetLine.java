package mz.multicore.erp.modules.accounting.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** One budget cell: amount planned for an account of class 6/7 in a cost centre and month (SPEC §4.1). */
@Entity
@Table(name = "budget_lines", uniqueConstraints = @UniqueConstraint(
        name = "uk_budget_lines_cell", columnNames = {"budget_id", "account_id", "cost_center_id", "budget_month"}))
@Getter
@Setter
public class BudgetLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id", nullable = false)
    private CostCenter costCenter;

    @Column(name = "budget_month", nullable = false)
    private int month;

    @Column(name = "amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal amount;

    public CellKey cellKey() {
        return new CellKey(account.getCode(), costCenter.getId(), month);
    }

    /** Identity of a cell inside one budget: account × cost centre × month. */
    public record CellKey(String accountCode, Long costCenterId, int month) {}
}
