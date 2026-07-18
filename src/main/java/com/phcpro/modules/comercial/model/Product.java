package com.phcpro.modules.comercial.model;

import com.phcpro.architecture.BaseEntity;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.fiscal.model.TaxRate;
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

    /** Preço de venda ao grosso (opcional). Aplica-se quando a quantidade da linha ≥ {@link #wholesaleMinQty}. */
    @Column(name = "wholesale_price")
    private BigDecimal wholesalePrice;

    /** Quantidade mínima (em unidades) a partir da qual se aplica o {@link #wholesalePrice}. */
    @Column(name = "wholesale_min_qty")
    private BigDecimal wholesaleMinQty;

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

    /**
     * Taxa de IVA do produto (IVA dinâmico). Aponta para uma {@link TaxRate} configurável
     * (normal 16%, reduzida, isento). Quando nula, aplica-se a taxa-padrão do sistema.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_rate_id")
    private TaxRate taxRate;

    @Column(name = "description", length = 500)
    private String description;

    /** Imagem do produto (thumbnail ~320px) para o catálogo POS em cards. */
    @Column(name = "image_data")
    private byte[] imageData;

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

    /**
     * Preço unitário efectivo para uma dada quantidade: aplica o preço de grosso quando definido e a
     * quantidade atinge a mínima de grosso; caso contrário, o preço de retalho ({@link #unitPrice}).
     * Regra pura de domínio (sem IO) — usada por faturação, encomenda e POS.
     */
    public BigDecimal effectiveUnitPrice(BigDecimal quantity) {
        if (wholesalePrice != null && wholesaleMinQty != null && quantity != null
                && wholesaleMinQty.signum() > 0
                && quantity.compareTo(wholesaleMinQty) >= 0) {
            return wholesalePrice;
        }
        return unitPrice;
    }
}
