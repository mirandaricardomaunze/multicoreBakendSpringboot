package com.phcpro.modules.comercial.model;

import com.phcpro.architecture.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku", nullable = false, unique = true)
    private String sku;

    @Column(name = "reference", unique = true)
    private String reference;

    @Column(name = "barcode", unique = true)
    private String barcode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "purchase_price")
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @Column(name = "min_stock")
    private BigDecimal minStock = BigDecimal.ZERO;

    /** Unidades por caixa para conversão visual (Qtd Caixas = stock / unitsPerBox). Default 1. */
    @Column(name = "units_per_box", nullable = false)
    private int unitsPerBox = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ProductCategory category;

    @Column(name = "description", length = 500)
    private String description;
}
