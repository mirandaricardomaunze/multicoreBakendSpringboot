package com.phcpro.modules.purchases.model;

import com.phcpro.architecture.BaseEntity;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.inventory.model.Warehouse;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Encomenda a fornecedor (purchase order). Pedido formal antes da entrega física.
 * Não move stock na criação — só a recepção ({@code RECEIVED}) gera entradas de stock.
 * Ciclo: {@code ORDERED → RECEIVED} / {@code CANCELLED}.
 */
@Entity
@Table(name = "purchase_orders", uniqueConstraints = @UniqueConstraint(
        name = "uk_purchase_orders_company_number", columnNames = {"company_id", "order_number"}))
@Getter
@Setter
public class PurchaseOrder extends BaseEntity {

    public static final String ORDERED = "ORDERED";
    public static final String PARTIALLY_RECEIVED = "PARTIALLY_RECEIVED";
    public static final String RECEIVED = "RECEIVED";
    public static final String CANCELLED = "CANCELLED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "expected_date")
    private LocalDate expectedDate;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate = LocalDateTime.now();

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "status", nullable = false)
    private String status = ORDERED;

    @Column(name = "notes")
    private String notes;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderLine> lines = new ArrayList<>();

    public void addLine(PurchaseOrderLine line) {
        line.setPurchaseOrder(this);
        this.lines.add(line);
    }
}
