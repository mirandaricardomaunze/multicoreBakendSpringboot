package mz.multicore.erp.modules.accounting.model;

import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Lançamento no diário — um facto contabilístico com as suas partidas.
 *
 * <p>Numeração própria (série {@code LC}) por empresa, como os restantes documentos (V31).
 */
@Entity
@Table(name = "journal_entries", uniqueConstraints = @UniqueConstraint(
        name = "uk_journal_entries_company_number", columnNames = {"company_id", "entry_number"}))
@Getter
@Setter
public class JournalEntry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_number", nullable = false, length = 40)
    private String entryNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /** Data do facto (não a data em que se registou). */
    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "description", nullable = false, length = 300)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    private JournalSource source = JournalSource.MANUAL;

    /** Id do documento de origem (fatura, recibo, compra), quando o lançamento é automático. */
    @Column(name = "source_document_id")
    private Long sourceDocumentId;

    /** Número do documento de origem, para o razão ser legível sem ir buscar o documento. */
    @Column(name = "source_document_number", length = 40)
    private String sourceDocumentNumber;

    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalLine> lines = new ArrayList<>();

    public void addLine(JournalLine line) {
        lines.add(line);
        line.setEntry(this);
    }

    public BigDecimal totalDebit() {
        return lines.stream().map(JournalLine::safeDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalCredit() {
        return lines.stream().map(JournalLine::safeCredit).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Débito igual a crédito — a condição que define uma partida dobrada. */
    public boolean isBalanced() {
        return totalDebit().compareTo(totalCredit()) == 0;
    }

    /**
     * <b>Fonte única</b> da validação de um lançamento. Chamada antes de gravar, venha o
     * lançamento de onde vier (manual ou automático) — um lançamento desequilibrado gravado é
     * um balancete que nunca mais fecha, e ninguém consegue dizer quando começou.
     */
    public void validateForPosting() {
        if (lines.isEmpty()) {
            throw new BusinessRuleException("O lançamento tem de ter pelo menos duas partidas.");
        }
        if (lines.size() < 2) {
            throw new BusinessRuleException("Partida dobrada: um lançamento precisa de pelo menos duas partidas.");
        }
        if (entryDate == null) {
            throw new BusinessRuleException("A data do lançamento é obrigatória.");
        }
        for (JournalLine line : lines) {
            line.validate();
        }
        if (totalDebit().signum() <= 0) {
            throw new BusinessRuleException("O lançamento não movimenta valor nenhum.");
        }
        if (!isBalanced()) {
            throw new BusinessRuleException(String.format(
                    "Lançamento desequilibrado: débito %s ≠ crédito %s.",
                    totalDebit().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                    totalCredit().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()));
        }
    }
}
