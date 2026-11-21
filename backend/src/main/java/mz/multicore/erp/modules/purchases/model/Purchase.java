package mz.multicore.erp.modules.purchases.model;

import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchases", uniqueConstraints = {
        @UniqueConstraint(name = "uk_purchases_company_number", columnNames = {"company_id", "purchase_number"}),
        @UniqueConstraint(name = "uk_purchases_company_supplier_original",
                columnNames = {"company_id", "supplier_id", "original_number"})})
@Getter
@Setter
public class Purchase extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "purchase_number", nullable = false)
    private String purchaseNumber; // e.g. V/FT-2026/1

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /** Armazém onde a mercadoria entrou; vazio no Saldo Inicial Fornecedor, que não movimenta stock. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    /** Compra ou "Saldo Inicial Fornecedor" importado (IMPORTACAO_DADOS_SPEC §5.3). */
    @Enumerated(EnumType.STRING)
    @Column(name = "document_kind", nullable = false, length = 20,
            columnDefinition = "varchar(20) default 'PURCHASE' not null")
    private PurchaseDocumentKind documentKind = PurchaseDocumentKind.PURCHASE;

    /** Número da factura no sistema antigo; só no Saldo Inicial, onde com o fornecedor forma a chave da importação. */
    @Column(name = "original_number", length = 60)
    private String originalNumber;

    /** Vencimento da factura antiga no Saldo Inicial Fornecedor. */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /** Descrição livre do Saldo Inicial Fornecedor, trazida do ficheiro importado. */
    @Column(name = "description", length = 300)
    private String description;

    /** Encomenda cuja recepção criou esta compra; vazio na compra directa. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id")
    private PurchaseOrder purchaseOrder;

    @Column(name = "purchase_date", nullable = false)
    private LocalDateTime purchaseDate = LocalDateTime.now();

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "status", nullable = false)
    private String status = "COMPLETED"; // COMPLETED, CANCELLED

    /** Valor devolvido ao fornecedor (notas DF): abate ao que se deve. */
    @Column(name = "returned_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal returnedAmount = BigDecimal.ZERO;

    @Transient
    public BigDecimal getOutstanding() {
        BigDecimal total = totalAmount == null ? BigDecimal.ZERO : totalAmount;
        BigDecimal paid = amountPaid == null ? BigDecimal.ZERO : amountPaid;
        BigDecimal returned = returnedAmount == null ? BigDecimal.ZERO : returnedAmount;
        BigDecimal out = total.subtract(returned).subtract(paid);
        return out.signum() > 0 ? out : BigDecimal.ZERO;
    }

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseLine> lines = new ArrayList<>();

    public void addLine(PurchaseLine line) {
        lines.add(line);
        line.setPurchase(this);
    }
}
