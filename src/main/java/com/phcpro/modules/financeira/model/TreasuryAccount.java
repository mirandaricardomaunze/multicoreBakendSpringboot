package com.phcpro.modules.financeira.model;

import com.phcpro.architecture.BaseEntity;
import com.phcpro.modules.company.model.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "treasury_accounts")
@Getter
@Setter
public class TreasuryAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Optimistic locking: protege o saldo contra movimentos de tesouraria concorrentes.
    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "name", nullable = false)
    private String name; // e.g. "Caixa Geral de Depósitos", "Caixa Geral"

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "balance", nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
}
