package com.phcpro.modules.comercial.model;

import com.phcpro.architecture.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.inventory.model.Warehouse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices", uniqueConstraints = @UniqueConstraint(
        name = "uk_invoices_company_number", columnNames = {"company_id", "invoice_number"}))
@Getter
@Setter
public class Invoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    /** Nome do comprador a mostrar no recibo. Para vendas de balcão (walk-in) guarda o nome
     *  escrito pelo operador; quando nulo, o recibo usa o nome do cliente registado. */
    @Column(name = "customer_name", length = 120)
    private String customerName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "total_before_tax", nullable = false)
    private BigDecimal totalBeforeTax = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "amount_paid", nullable = false, precision = 14, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "sales_channel", nullable = false)
    private SalesChannel salesChannel = SalesChannel.MANUAL;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceLine> lines = new ArrayList<>();

    @Column(name = "rejection_reason")
    private String rejectionReason;

    public void addLine(InvoiceLine line) {
        lines.add(line);
        line.setInvoice(this);
    }

    /**
     * Saldo por liquidar (total − recebido), nunca negativo.
     * É este o valor que o cliente ainda deve — não o total da fatura.
     */
    public BigDecimal outstandingAmount() {
        BigDecimal remaining = safeTotal().subtract(safePaid());
        return remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO;
    }

    /**
     * <b>Fonte única</b> do estado de pagamento de uma fatura já emitida — a mesma regra no
     * POS, na emissão de recibo e na tesouraria (mesmo padrão de
     * {@code Product.effectiveTaxRate()}).
     *
     * <p>Chamar só depois de actualizar {@code amountPaid}, e só sobre faturas emitidas:
     * um documento anulado, rejeitado ou à espera de aprovação mantém o seu estado.
     */
    public InvoiceStatus deriveStatusFromPayments() {
        BigDecimal paid = safePaid();
        if (paid.compareTo(safeTotal()) >= 0)    return InvoiceStatus.PAID;
        if (paid.compareTo(BigDecimal.ZERO) > 0) return InvoiceStatus.PARTIALLY_PAID;
        return InvoiceStatus.APPROVED; // fiado — emitida e por receber
    }

    private BigDecimal safeTotal() {
        return totalAmount == null ? BigDecimal.ZERO : totalAmount;
    }

    private BigDecimal safePaid() {
        return amountPaid == null ? BigDecimal.ZERO : amountPaid;
    }
}
