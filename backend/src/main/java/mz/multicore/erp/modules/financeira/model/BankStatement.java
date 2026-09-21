package mz.multicore.erp.modules.financeira.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bank_statements")
@Getter
@Setter
public class BankStatement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "treasury_account_id", nullable = false)
    private TreasuryAccount treasuryAccount;

    @Column(name = "statement_reference", nullable = false, length = 80)
    private String statementReference;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "opening_balance", nullable = false, precision = 14, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(name = "closing_balance", nullable = false, precision = 14, scale = 2)
    private BigDecimal closingBalance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BankReconciliationStatus status = BankReconciliationStatus.OPEN;

    @Column(name = "notes", length = 500)
    private String notes;

    @OneToMany(mappedBy = "bankStatement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BankStatementItem> items = new ArrayList<>();

    public void addItem(BankStatementItem item) {
        items.add(item);
        item.setBankStatement(this);
    }

    public BigDecimal calculatedBalance() {
        BigDecimal sum = openingBalance != null ? openingBalance : BigDecimal.ZERO;
        for (BankStatementItem it : items) {
            if (it.getAmount() != null) {
                sum = sum.add(it.getAmount());
            }
        }
        return sum;
    }

    public BigDecimal difference() {
        BigDecimal target = closingBalance != null ? closingBalance : BigDecimal.ZERO;
        return calculatedBalance().subtract(target);
    }
}
