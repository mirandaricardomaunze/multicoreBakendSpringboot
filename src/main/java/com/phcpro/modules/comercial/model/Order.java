package com.phcpro.modules.comercial.model;

import com.phcpro.architecture.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.inventory.model.Warehouse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_orders")
@Getter
@Setter
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true)
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
    private String status = "PENDING"; // PENDING, BILLED, CANCELLED

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> lines = new ArrayList<>();

    @Column(name = "invoice_id")
    private Long invoiceId;

    /** Timestamp da última impressão da encomenda (null se nunca foi impressa). */
    @Column(name = "printed_at")
    private LocalDateTime printedAt;

    /** Quantas vezes a encomenda foi impressa. Usado pelo UI para avisar de re-impressões. */
    @Column(name = "print_count", nullable = false)
    private int printCount = 0;

    /** Username do operador que fez a última impressão. */
    @Column(name = "last_printed_by", length = 80)
    private String lastPrintedBy;

    public void addLine(OrderLine line) {
        lines.add(line);
        line.setOrder(this);
    }
}
