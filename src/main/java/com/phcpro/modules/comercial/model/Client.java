package com.phcpro.modules.comercial.model;

import com.phcpro.architecture.BaseEntity;
import com.phcpro.modules.company.model.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "clients")
@Getter
@Setter
public class Client extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "tax_id", nullable = false, unique = true)
    private String taxId; // NIF in Portugal / CNPJ/CPF in Brazil

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "address")
    private String address;

    /**
     * Prazo de pagamento acordado, em dias. Zero (o default) é pronto pagamento — o
     * comportamento de toda a base anterior à V35, pelo que nada muda para quem não o definir.
     */
    @Column(name = "payment_terms_days", nullable = false)
    private Integer paymentTermsDays = 0;

    /**
     * Tecto de dívida em aberto. <b>Nulo = sem limite</b> (crédito livre, o comportamento de
     * toda a base anterior à V36). Zero é diferente de nulo: significa "não vende fiado".
     */
    @Column(name = "credit_limit", precision = 14, scale = 2)
    private BigDecimal creditLimit;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "client_companies",
            joinColumns = @JoinColumn(name = "client_id"),
            inverseJoinColumns = @JoinColumn(name = "company_id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_client_company", columnNames = {"client_id", "company_id"})
    )
    private Set<Company> companies = new LinkedHashSet<>();

    public boolean belongsToCompany(Long companyId) {
        return companyId != null && companies.stream().anyMatch(company -> companyId.equals(company.getId()));
    }

    /** Prazo de pagamento em dias, nunca nulo nem negativo (mesmo padrão de {@code effectiveTaxRate}). */
    public int effectivePaymentTermsDays() {
        return paymentTermsDays == null || paymentTermsDays < 0 ? 0 : paymentTermsDays;
    }

    /** Tem tecto de dívida definido. Sem tecto, o crédito é livre — não é o mesmo que tecto zero. */
    public boolean hasCreditLimit() {
        return creditLimit != null;
    }

    /**
     * Quanto ainda pode levar fiado, dada a dívida actual. {@code null} quando não há limite
     * definido (crédito livre). Nunca negativo: quem já estourou o limite tem zero disponível.
     */
    public BigDecimal creditAvailable(BigDecimal currentDebt) {
        if (!hasCreditLimit()) return null;
        BigDecimal debt = currentDebt == null ? BigDecimal.ZERO : currentDebt;
        BigDecimal available = creditLimit.subtract(debt);
        return available.signum() >= 0 ? available : BigDecimal.ZERO;
    }

    /**
     * <b>Fonte única</b> da decisão "esta venda a fiado cabe no limite?".
     *
     * @param currentDebt dívida já em aberto do cliente
     * @param newDebt     valor que esta venda vai acrescentar à dívida
     */
    public boolean exceedsCreditLimit(BigDecimal currentDebt, BigDecimal newDebt) {
        if (!hasCreditLimit()) return false;
        BigDecimal debt = currentDebt == null ? BigDecimal.ZERO : currentDebt;
        BigDecimal addition = newDebt == null ? BigDecimal.ZERO : newDebt;
        if (addition.signum() <= 0) return false; // venda paga na hora não consome crédito
        return debt.add(addition).compareTo(creditLimit) > 0;
    }
}
