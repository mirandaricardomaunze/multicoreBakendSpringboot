package mz.multicore.erp.modules.comercial.model;

import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.inventory.model.Warehouse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_orders", uniqueConstraints = @UniqueConstraint(
        name = "uk_customer_orders_company_number", columnNames = {"company_id", "order_number"}))
@Getter
@Setter
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number")
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    /** Nome livre do comprador quando a encomenda é para "Consumidor Final" — não cria registo
     *  de cliente, só serve como rótulo no recibo / consulta. Null para clientes registados. */
    @Column(name = "walk_in_name", length = 120)
    private String walkInName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column(name = "total_before_tax", nullable = false)
    private BigDecimal totalBeforeTax = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "status", nullable = false)
    private String status = "PENDING"; // PENDING_APPROVAL, PENDING, GUIDE_PENDING, GUIDED, BILLED, CANCELLED

    /**
     * Via da encomenda — decide aprovação, formato do documento e se entra na separação.
     * Declarada na criação e imutável a partir daí (R6). Ver {@link OrderKind}.
     */
    /**
     * Largura 40 e não 20: {@code INTERNAL_REPLENISHMENT} tem 22 caracteres e não cabia na coluna
     * criada pela V43, pelo que a via nova rebentava ao gravar. Folga deliberada para a próxima via
     * não repetir o mesmo. Ver V46.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 40)
    private OrderKind kind = OrderKind.FORMAL_ORDER;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> lines = new ArrayList<>();

    @Column(name = "invoice_id")
    private Long invoiceId;

    // ─── Origem e condições acordadas (ver docs/ENCOMENDA_PROFISSIONAL_SPEC.md) ──
    /** Cotação que deu origem a esta encomenda. Nulo = encomenda criada sem acordo prévio. */
    @Column(name = "quotation_id")
    private Long quotationId;

    @Column(name = "quotation_number")
    private String quotationNumber;

    /** Condições copiadas da cotação na conversão — nunca lidas por referência: um compromisso
     *  assumido não muda porque o documento de origem mudou. */
    @Column(name = "payment_terms", length = 200)
    private String paymentTerms;

    @Column(name = "delivery_terms", length = 200)
    private String deliveryTerms;

    /** Data prometida ao cliente, calculada na confirmação e <b>gravada</b> (molde do
     *  {@code Invoice.dueDate}): mudar o prazo acordado amanhã não altera o que já foi prometido. */
    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    /** Guia de remessa gerada a partir desta encomenda (null enquanto não houver guia activa).
     *  Caminho separado da faturação: uma encomenda vira guia OU fatura, nunca as duas. */
    @Column(name = "delivery_guide_id")
    private Long deliveryGuideId;

    // ─── Reposição interna (ver docs/REPOSICAO_INTERNA_SPEC.md) ──────────────
    /**
     * Armazém que recebe — a loja que pediu. Obrigatório na reposição interna, nulo em tudo o resto.
     *
     * <p>O {@link #warehouse} continua a ser a origem. Gravar o destino no pedido é o que garante
     * que a mercadoria chega a quem a pediu: sem ele, a conversão teria de o perguntar outra vez.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_warehouse_id")
    private Warehouse destinationWarehouse;

    /** Transferência gerada a partir desta encomenda (null enquanto não houver). */
    @Column(name = "stock_transfer_id")
    private Long stockTransferId;

    @Column(name = "transfer_number", length = 40)
    private String transferNumber;

    /** Timestamp da última impressão da encomenda (null se nunca foi impressa). */
    @Column(name = "printed_at")
    private LocalDateTime printedAt;

    /** Quantas vezes a encomenda foi impressa. Usado pelo UI para avisar de re-impressões. */
    @Column(name = "print_count", nullable = false)
    private int printCount = 0;

    /** Username do operador que fez a última impressão. */
    @Column(name = "last_printed_by", length = 80)
    private String lastPrintedBy;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "reservation_active", nullable = false)
    private boolean reservationActive;

    @Version
    private long version;

    public void addLine(OrderLine line) {
        lines.add(line);
        line.setOrder(this);
    }

    /**
     * Calcula e grava a data de entrega prometida. Regra pura de domínio, sem IO — mesmo molde do
     * {@code Invoice.assignDueDate}.
     *
     * @param confirmedOn data da confirmação (é daqui que o prazo conta, não da aprovação interna)
     * @param days        dias acordados; <b>nulo deixa a data por definir</b>, que é o caso de quem
     *                    não prometeu nenhuma (o comportamento de toda a base anterior à V45)
     */
    public void assignExpectedDelivery(LocalDate confirmedOn, Integer days) {
        if (days == null) {
            this.expectedDeliveryDate = null;
            return;
        }
        if (confirmedOn == null) {
            throw new BusinessRuleException("Data de confirmação obrigatória para calcular a entrega.");
        }
        if (days <= 0) {
            throw new BusinessRuleException("O prazo de entrega deve ser de pelo menos um dia.");
        }
        this.expectedDeliveryDate = confirmedOn.plusDays(days);
    }

    /**
     * Passou a data prometida e a encomenda ainda não foi entregue nem fechada. O que já está
     * facturado, expedido por guia ou cancelado não está em atraso — está feito.
     */
    public boolean isDeliveryOverdue(LocalDate today) {
        if (today == null || expectedDeliveryDate == null || !today.isAfter(expectedDeliveryDate)) {
            return false;
        }
        return !"BILLED".equalsIgnoreCase(status)
                && !"GUIDED".equalsIgnoreCase(status)
                && !"CANCELLED".equalsIgnoreCase(status);
    }
}
