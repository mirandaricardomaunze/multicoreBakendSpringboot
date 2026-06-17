package com.phcpro.modules.comercial.model;

import com.phcpro.architecture.BaseEntity;
import com.phcpro.modules.company.model.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_type", nullable = false, length = 20)
    private ProductSaleType saleType = ProductSaleType.UNIT;

    @Column(name = "stock_tracked", nullable = false)
    private boolean stockTracked = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ProductCategory category;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "product_companies",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "company_id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_product_company", columnNames = {"product_id", "company_id"})
    )
    private Set<Company> companies = new LinkedHashSet<>();

    public boolean belongsToCompany(Long companyId) {
        return companyId != null && companies.stream().anyMatch(company -> companyId.equals(company.getId()));
    }
}
