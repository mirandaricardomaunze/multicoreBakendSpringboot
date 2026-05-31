package com.phcpro.modules.financeira.model;

import com.phcpro.architecture.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "treasury_transactions")
@Getter
@Setter
public class TreasuryTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private TreasuryAccount treasuryAccount;

    @Column(name = "transaction_type", nullable = false)
    private String transactionType; // "DEBIT" (Inflow/Receita) or "CREDIT" (Outflow/Despesa)

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate = LocalDateTime.now();
}
