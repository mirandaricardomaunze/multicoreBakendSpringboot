package com.phcpro.modules.purchases.model;

import com.phcpro.modules.comercial.model.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Linha de uma encomenda a fornecedor. Lote/validade são a intenção de recepção; a entrada
 * efectiva de stock acontece na recepção da encomenda.
 */
@Entity
@Table(name = "purchase_order_lines")
@Getter
@Setter
public class PurchaseOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;

    /** Quantidade já recebida em stock (recepção parcial). Em falta = quantity − receivedQuantity. */
    @Column(name = "received_quantity", nullable = false)
    private BigDecimal receivedQuantity = BigDecimal.ZERO;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "tax_rate", nullable = false)
    private BigDecimal taxRate;

    @Column(name = "line_total", nullable = false)
    private BigDecimal lineTotal;

    @Column(name = "batch_number")
    private String batchNumber;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "serial_number")
    private String serialNumber;
}
