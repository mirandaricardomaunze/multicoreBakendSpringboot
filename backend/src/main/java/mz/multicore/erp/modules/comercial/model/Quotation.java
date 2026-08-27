package mz.multicore.erp.modules.comercial.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.inventory.model.Warehouse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Cotação ao cliente — a proposta de preço que antecede a encomenda. Numeração série {@code CT},
 * única por empresa. Não move stock, dívida, caixa nem contabilidade: o compromisso nasce na
 * conversão em encomenda. Ver docs/COTACAO_SPEC.md.
 */
@Entity
@Table(name = "quotations", uniqueConstraints = @UniqueConstraint(
        name = "uk_quotations_company_number", columnNames = {"company_id", "quotation_number"}))
@Getter
@Setter
public class Quotation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quotation_number", nullable = false)
    private String quotationNumber;

    @Column(name = "quotation_date", nullable = false)
    private LocalDateTime quotationDate;

    /**
     * Último dia em que o preço proposto é honrado. <b>Gravada no documento</b>: mudar o default do
     * sistema não altera propostas já emitidas — a mesma lição do {@code Invoice.dueDate} (V35).
     */
    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    /** Rótulo livre do comprador quando a cotação é para "Consumidor Final" (sem cliente registado). */
    @Column(name = "walk_in_name", length = 120)
    private String walkInName;

    /**
     * Armazém de onde a mercadoria sairá se a proposta for aceite. Existe porque a conversão em
     * encomenda precisa dele — é informação interna e por isso <b>não sai no PDF</b> do cliente.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "total_before_tax", nullable = false)
    private BigDecimal totalBeforeTax = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private QuotationStatus status = QuotationStatus.DRAFT;

    // ─── Condições comerciais (livres; impressas quando preenchidas) ─────────
    @Column(name = "payment_terms", length = 200)
    private String paymentTerms;

    @Column(name = "delivery_terms", length = 200)
    private String deliveryTerms;

    /**
     * Prazo de entrega em <b>dias a contar da confirmação</b>. Não é uma data porque a cotação não
     * sabe quando o cliente vai confirmar — a data nasce na conversão
     * ({@code Order.assignExpectedDelivery}). Nulo = sem prazo prometido.
     *
     * <p>Coexiste com {@link #deliveryTerms} sem ser redundante: nem toda a promessa de entrega é um
     * número ("entrega faseada", "levantamento no armazém"). Os dias são o que o sistema calcula; o
     * texto é o que o cliente leu.
     */
    @Column(name = "delivery_days")
    private Integer deliveryDays;

    @Column(name = "notes", length = 1000)
    private String notes;

    // ─── Rasto da proposta ──────────────────────────────────────────────────
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "decided_by", length = 80)
    private String decidedBy;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    /** Encomenda gerada na conversão. Preenchido = esta proposta já foi convertida (terminal). */
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_number")
    private String orderNumber;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuotationLine> lines = new ArrayList<>();

    @Version
    private long version;

    public void addLine(QuotationLine line) {
        lines.add(line);
        line.setQuotation(this);
    }

    /**
     * Calcula e grava a validade a partir da data de emissão. Regra pura de domínio, sem IO —
     * mesmo molde do {@code Invoice.assignDueDate}.
     *
     * @param issueDate    data de emissão
     * @param requestedDays dias de validade pedidos; nulo aplica {@link QuotationValidity#DEFAULT_DAYS}
     */
    public void assignValidity(LocalDate issueDate, Integer requestedDays) {
        if (issueDate == null) {
            throw new BusinessRuleException("Data de emissão obrigatória para calcular a validade.");
        }
        int days = requestedDays == null ? QuotationValidity.DEFAULT_DAYS : requestedDays;
        if (days <= 0) {
            throw new BusinessRuleException("A validade da cotação deve ser de pelo menos um dia.");
        }
        this.validUntil = issueDate.plusDays(days);
    }

    /**
     * <b>Fonte única</b> da pergunta "este preço ainda é para honrar?". O último dia de validade
     * ainda conta como válido — quem recebe uma proposta "válida até 30/09" espera poder aceitá-la
     * no dia 30.
     */
    public boolean isExpired(LocalDate today) {
        return today != null && validUntil != null && today.isAfter(validUntil);
    }

    /** Dias que faltam para caducar; zero no último dia, negativo depois. */
    public long daysUntilExpiry(LocalDate today) {
        if (today == null || validUntil == null) return 0;
        return ChronoUnit.DAYS.between(today, validUntil);
    }

    /**
     * A proposta está viva e dentro do prazo — as duas condições que a conversão exige.
     * Estar convertida, recusada, cancelada ou caducada responde {@code false}.
     */
    public boolean isConvertible(LocalDate today) {
        return status.isOpen() && !isExpired(today);
    }

    /**
     * O que esta proposta prometeu, para a encomenda gerada na conversão o herdar. É a cotação quem
     * sabe o que acordou — daí ser ela a dizê-lo, em vez de o serviço remontar os campos à mão.
     */
    public OrderTerms agreedTerms() {
        return new OrderTerms(id, quotationNumber, paymentTerms, deliveryTerms, deliveryDays);
    }

    /** Rótulo do comprador: o nome livre quando não há cliente registado. */
    public String clientLabel() {
        if (walkInName != null && !walkInName.isBlank()) {
            return walkInName;
        }
        return client != null ? client.getName() : "";
    }
}
