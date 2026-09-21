package mz.multicore.erp.modules.financeira.model;

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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "bank_statement_items")
@Getter
@Setter
public class BankStatementItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bank_statement_id", nullable = false)
    private BankStatement bankStatement;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "value_date")
    private LocalDate valueDate;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "reference", length = 100)
    private String reference;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "balance_after", precision = 14, scale = 2)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BankStatementItemStatus status = BankStatementItemStatus.UNMATCHED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_transaction_id")
    private TreasuryTransaction matchedTransaction;

    @Column(name = "notes", length = 255)
    private String notes;
}
