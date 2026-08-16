package com.phcpro.modules.accounting.model;

import com.phcpro.architecture.BaseEntity;
import com.phcpro.modules.company.model.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Conta do plano de contas (PGC-NIRF), por empresa.
 *
 * <p>O plano é hierárquico pelo código: {@code 2} → {@code 21} → {@code 2101}. Só as folhas
 * ({@link #postable}) aceitam lançamentos; as contas-mãe existem para somar. Lançar numa
 * conta-mãe é o erro clássico que faz o balancete deixar de fechar por classes.
 */
@Entity
@Table(name = "accounts", uniqueConstraints = @UniqueConstraint(
        name = "uk_accounts_company_code", columnNames = {"company_id", "code"}))
@Getter
@Setter
public class Account extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código PGC-NIRF, ex.: {@code "1101"}. Único por empresa (numeração é por tenant, V31). */
    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_class", nullable = false, length = 40)
    private AccountClass accountClass;

    /**
     * Natureza do saldo. <b>Gravada na conta</b>, não derivada da classe: Clientes e
     * Fornecedores são ambos classe 2 e têm naturezas opostas.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "nature", nullable = false, length = 10)
    private AccountNature nature;

    /** Aceita lançamentos (conta folha). Contas-mãe são só agregadoras. */
    @Column(name = "postable", nullable = false)
    private boolean postable = true;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** Código da conta-mãe, ou {@code null} na raiz da classe. Derivado do próprio código. */
    @Column(name = "parent_code", length = 20)
    private String parentCode;

    /**
     * Código da conta-mãe segundo a convenção do plano: menos um dígito.
     * {@code "2101"} → {@code "210"}; {@code "2"} → {@code null}.
     */
    public static String parentCodeOf(String code) {
        if (code == null || code.trim().length() <= 1) return null;
        return code.trim().substring(0, code.trim().length() - 1);
    }
}
