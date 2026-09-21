package mz.multicore.erp.modules.financeira.model;

import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import mz.multicore.erp.modules.financeira.model.TreasuryAccountType;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 16)
    private TreasuryAccountType accountType = TreasuryAccountType.CASH;

    @Column(name = "balance", nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
}
