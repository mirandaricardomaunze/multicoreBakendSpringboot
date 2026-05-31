package com.phcpro.modules.financeira.model;

import com.phcpro.architecture.BaseEntity;
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

    @Column(name = "name", nullable = false)
    private String name; // e.g. "Caixa Geral de Depósitos", "Caixa Geral"

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "balance", nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;
}
