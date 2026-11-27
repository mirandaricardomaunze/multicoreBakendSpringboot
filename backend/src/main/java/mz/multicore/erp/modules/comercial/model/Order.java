package mz.multicore.erp.modules.comercial.model;

import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import mz.multicore.erp.modules.comercial.service.OrderReservationWatcher;
import mz.multicore.erp.modules.comercial.service.OrderStatusChangePublisher;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.inventory.model.Warehouse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "customer_orders", uniqueConstraints = @UniqueConstraint(
        name = "uk_customer_orders_company_number", columnNames = {"company_id", "order_number"}))
@EntityListeners({OrderStatusChangePublisher.class, OrderReservationWatcher.class})
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_unit_id")
    private mz.multicore.erp.modules.purchases.model.BusinessUnit businessUnit;

    @Column(name = "total_before_tax", nullable = false)
    private BigDecimal totalBeforeTax = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "status", nullable = false)
    /** Nome de um {@link OrderStatus}; texto na base, comparado sempre através do enum. */
    private String status = OrderStatus.PENDING.name();

    /**
     * Estado tal como está gravado na base (lido ou acabado de gravar) — serve só para saber, no fim do flush, se o
     * estado mudou de facto e avisar com {@code OrderStatusChangedEvent} (WH-02). Não é coluna.
     */
    @Transient
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String persistedStatus;

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

    @Column(name = "notes", length = 1000)
    private String notes;

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

    // ─── Origem externa (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.4) ──────────────
    /** Por onde entrou a encomenda; as do desktop ficam {@link OrderOrigin#DESKTOP}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false, length = 20,
            columnDefinition = "varchar(20) default 'DESKTOP' not null")
    private OrderOrigin origin = OrderOrigin.DESKTOP;

    /** Nome da chave de API que a criou (ex.: "Loja online"); nulo nas do desktop. */
    @Column(name = "origin_name", length = 120)
    private String originName;

    /** Referência do sistema externo (número da encomenda na loja online); nulo nas do desktop. */
    @Column(name = "external_reference", length = 80)
    private String externalReference;

    /** Telefone de contacto do Consumidor Final que encomendou por um sistema externo. */
    @Column(name = "contact_phone", length = 40)
    private String contactPhone;

    /** SHA-256 do corpo que criou a encomenda pela API (§3.4.2): repetição igual → 200, diferente → 409. */
    @Column(name = "request_fingerprint", length = 64)
    private String requestFingerprint;

    @Version
    private long version;

    /** Liberta o stock que a encomenda tinha prometido; nada muda se não houver reserva activa. */
    public void releaseReservation() {
        if (!reservationActive) {
            return;
        }
        reservationActive = false;
        lines.forEach(line -> line.setReservedQuantity(BigDecimal.ZERO));
    }

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
        return !OrderStatus.isClosed(status);
    }

    /**
     * Estado anterior quando o estado gravado mudou desde a última leitura/gravação, e passa a lembrar o actual;
     * vazio quando não mudou ou quando a encomenda acabou de ser criada (criar não é mudar de estado).
     */
    public Optional<String> takePersistedStatusChange() {
        String previous = persistedStatus;
        persistedStatus = status;
        if (previous == null || previous.equals(status)) {
            return Optional.empty();
        }
        return Optional.of(previous);
    }

    /** Lembra o estado tal como ficou gravado (depois de ler ou de criar a encomenda). */
    public void rememberPersistedStatus() {
        persistedStatus = status;
    }
}
