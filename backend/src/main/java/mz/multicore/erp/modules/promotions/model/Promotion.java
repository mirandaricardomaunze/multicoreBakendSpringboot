package mz.multicore.erp.modules.promotions.model;

import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.model.ProductCategory;
import mz.multicore.erp.modules.company.model.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Promoção de loja: aplica-se a um produto OU a uma categoria, durante uma janela de validade.
 * O alcance é exclusivo — exactamente um de {@link #product}/{@link #category} é preenchido.
 * O motor de cálculo (PromotionService) traduz a promoção num desconto efectivo por linha.
 */
@Entity
@Table(name = "promotions", indexes = {
        @Index(name = "idx_promo_company_active", columnList = "company_id, active")
})
@Getter
@Setter
public class Promotion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private PromotionType type;

    /** Alcance por produto (exclusivo com {@link #category}). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    /** Alcance por categoria (exclusivo com {@link #product}). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ProductCategory category;

    /** Percentagem de desconto (0–100) — usado por {@link PromotionType#PERCENT}. */
    @Column(name = "percent_value", precision = 5, scale = 2)
    private BigDecimal percentValue;

    /** "Leve X" — usado por {@link PromotionType#BUY_X_GET_Y}. */
    @Column(name = "buy_quantity")
    private Integer buyQuantity;

    /** "Pague Y" — usado por {@link PromotionType#BUY_X_GET_Y}. */
    @Column(name = "pay_quantity")
    private Integer payQuantity;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
