package mz.multicore.erp.modules.purchases.model;

import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.company.model.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Divergência entre o que se encomendou ao fornecedor e o que ele entregou.
 *
 * <p><b>Porque existe:</b> pagar 100 e receber 95 não é um roubo grande — são 3 sacos de cada
 * vez, todas as semanas, durante um ano. Sem um registo por ocorrência não há reclamação
 * possível nem forma de perceber que fornecedor entrega mal.
 *
 * <p>Uma linha por ocorrência (não é um saldo acumulado): a data e a quantidade daquele dia são
 * a prova. Guarda-se também o <b>valor</b>, porque é isso que se reclama.
 */
@Entity
@Table(name = "goods_receipt_discrepancies")
@Getter
@Setter
public class GoodsReceiptDiscrepancy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_line_id", nullable = false)
    private PurchaseOrderLine purchaseOrderLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Fornecedor, desnormalizado para o relatório não ter de atravessar a encomenda. */
    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "supplier_name", length = 200)
    private String supplierName;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private DiscrepancyType type;

    @Column(name = "quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    /** Preço unitário da encomenda — o que se pagou (ou vai pagar) por cada unidade. */
    @Column(name = "unit_price", precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "occurred_on", nullable = false)
    private LocalDate occurredOn;

    /** Já foi resolvida com o fornecedor (nota de crédito, substituição, perdoada). */
    @Column(name = "resolved", nullable = false)
    private boolean resolved = false;

    @Column(name = "resolution_notes", length = 500)
    private String resolutionNotes;

    /**
     * Valor em causa — quantidade × preço unitário. É este o número que se reclama, e o que
     * torna visível quanto é que "faltar 3 de vez em quando" custa ao fim do ano.
     */
    public BigDecimal amount() {
        BigDecimal qty = quantity == null ? BigDecimal.ZERO : quantity;
        BigDecimal price = unitPrice == null ? BigDecimal.ZERO : unitPrice;
        return qty.multiply(price);
    }
}
