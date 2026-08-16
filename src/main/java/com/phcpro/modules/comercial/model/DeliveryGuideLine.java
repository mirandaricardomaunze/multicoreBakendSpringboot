package com.phcpro.modules.comercial.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Linha da Guia de Remessa — cópia da linha da encomenda (o dinheiro é meramente informativo na
 * guia; o movimento fiscal fica para uma eventual fatura, que é caminho separado).
 */
@Entity
@Table(name = "delivery_guide_lines")
@Getter
@Setter
public class DeliveryGuideLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id", nullable = false)
    private DeliveryGuide guide;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "tax_rate", nullable = false)
    private BigDecimal taxRate;

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false)
    private BigDecimal lineTotal;

    @Column(name = "batch_number")
    private String batchNumber;

    @Column(name = "serial_number")
    private String serialNumber;
}
