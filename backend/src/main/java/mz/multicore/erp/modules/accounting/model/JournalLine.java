package mz.multicore.erp.modules.accounting.model;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Partida de um lançamento: uma conta, e um valor <b>ou</b> a débito <b>ou</b> a crédito.
 * Nunca os dois — uma partida com os dois lados preenchidos é ambígua e é assim que entram
 * lançamentos que parecem equilibrados sem o estar.
 */
@Entity
@Table(name = "journal_lines")
@Getter
@Setter
public class JournalLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false)
    private JournalEntry entry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "debit", precision = 16, scale = 2)
    private BigDecimal debit;

    @Column(name = "credit", precision = 16, scale = 2)
    private BigDecimal credit;

    @Column(name = "description", length = 300)
    private String description;

    /** Centro de custo da partida (classes 6 e 7; docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §3.2). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    public BigDecimal safeDebit() {
        return debit == null ? BigDecimal.ZERO : debit;
    }

    public BigDecimal safeCredit() {
        return credit == null ? BigDecimal.ZERO : credit;
    }

    /** Partida bem formada: conta movimentável, um só lado, valor positivo. */
    public void validate() {
        if (account == null) {
            throw new BusinessRuleException("Cada partida tem de indicar uma conta.");
        }
        if (!account.isPostable()) {
            throw new BusinessRuleException("A conta " + account.getCode() + " ("
                    + account.getName() + ") é uma conta-mãe e não aceita lançamentos.");
        }
        if (!account.isActive()) {
            throw new BusinessRuleException("A conta " + account.getCode() + " está inactiva.");
        }
        boolean hasDebit = safeDebit().signum() != 0;
        boolean hasCredit = safeCredit().signum() != 0;
        if (hasDebit && hasCredit) {
            throw new BusinessRuleException("A partida da conta " + account.getCode()
                    + " tem débito e crédito ao mesmo tempo — indique só um dos lados.");
        }
        if (!hasDebit && !hasCredit) {
            throw new BusinessRuleException("A partida da conta " + account.getCode() + " não tem valor.");
        }
        if (safeDebit().signum() < 0 || safeCredit().signum() < 0) {
            throw new BusinessRuleException("Valores negativos não são partidas — inverta o lado do lançamento.");
        }
    }

    /** Conta das classes 6 ou 7: a partida tem de levar centro de custo (§3.2). */
    public boolean requiresCostCenter() {
        int digit = accountClassDigit();
        return digit == 6 || digit == 7;
    }

    /**
     * Centro de custo bem aplicado (CC-04): obrigatório nas classes 6 e 7, proibido nas classes 1–5, e só um centro
     * activo da empresa do lançamento.
     */
    public void validateCostCenter(Long companyId) {
        int digit = accountClassDigit();
        if (costCenter == null) {
            if (requiresCostCenter()) {
                throw new BusinessRuleException("A partida da conta " + account.getCode()
                        + " (classe " + digit + ") tem de indicar o centro de custo.");
            }
            return;
        }
        if (digit >= 1 && digit <= 5) {
            throw new BusinessRuleException("A partida da conta " + account.getCode()
                    + " (classe " + digit + ") não leva centro de custo — só as classes 6 e 7.");
        }
        if (!costCenter.belongsTo(companyId)) {
            throw new BusinessRuleException("O centro de custo " + costCenter.getCode()
                    + " é de outra empresa e não pode ser usado neste lançamento.");
        }
        if (!costCenter.isActive()) {
            throw new BusinessRuleException("O centro de custo " + costCenter.getCode()
                    + " está inactivo e não aceita lançamentos.");
        }
    }

    private int accountClassDigit() {
        if (account == null) {
            return 0;
        }
        return account.getAccountClass() != null
                ? account.getAccountClass().digit()
                : AccountClass.ofCode(account.getCode()).digit();
    }

    /** Fábrica de uma partida a débito. */
    public static JournalLine debit(Account account, BigDecimal amount, String description) {
        JournalLine line = new JournalLine();
        line.setAccount(account);
        line.setDebit(amount);
        line.setDescription(description);
        return line;
    }

    /** Fábrica de uma partida a crédito. */
    public static JournalLine credit(Account account, BigDecimal amount, String description) {
        JournalLine line = new JournalLine();
        line.setAccount(account);
        line.setCredit(amount);
        line.setDescription(description);
        return line;
    }
}
