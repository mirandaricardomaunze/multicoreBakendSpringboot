package mz.multicore.erp.modules.accounting.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Annual budget of a company (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4.1): one per company and fiscal year, with one
 * line per account × cost centre × month.
 */
@Entity
@Table(name = "budgets", uniqueConstraints = @UniqueConstraint(
        name = "uk_budgets_company_year", columnNames = {"company_id", "fiscal_year"}))
@Getter
@Setter
public class Budget extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "fiscal_year", nullable = false)
    private int fiscalYear;

    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BudgetLine> lines = new ArrayList<>();

    public void addLine(BudgetLine line) {
        line.setBudget(this);
        lines.add(line);
    }

    /**
     * Makes the budget hold exactly {@code incoming}: existing cells keep their row and take the new amount, missing
     * cells are removed and new cells are added. Updating in place avoids a delete/insert clash on the unique cell key.
     */
    public void replaceLines(List<BudgetLine> incoming) {
        Map<BudgetLine.CellKey, BudgetLine> byKey = incoming.stream()
                .collect(Collectors.toMap(BudgetLine::cellKey, Function.identity()));
        lines.removeIf(existing -> !byKey.containsKey(existing.cellKey()));
        for (BudgetLine existing : lines) {
            existing.setAmount(byKey.remove(existing.cellKey()).getAmount());
        }
        incoming.stream().filter(line -> byKey.containsKey(line.cellKey())).forEach(this::addLine);
    }
}
