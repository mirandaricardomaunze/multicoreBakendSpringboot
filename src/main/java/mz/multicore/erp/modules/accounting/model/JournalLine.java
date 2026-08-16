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
