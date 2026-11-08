package mz.multicore.erp.modules.butchery.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.comercial.model.Product;

import java.math.BigDecimal;

/** A cut obtained in a breakdown, with the share of the carcass cost it carries. */
@Entity
@Table(name = "carcass_breakdown_lines")
@Getter
@Setter
public class CarcassBreakdownLine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "breakdown_id", nullable = false)
    private CarcassBreakdown breakdown;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "kg", nullable = false, precision = 14, scale = 3)
    private BigDecimal kg;

    @Column(name = "unit_sale_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitSalePrice = BigDecimal.ZERO;

    @Column(name = "allocated_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal allocatedCost = BigDecimal.ZERO;

    @Column(name = "cost_per_kg", nullable = false, precision = 14, scale = 4)
    private BigDecimal costPerKg = BigDecimal.ZERO;

    @Column(name = "expected_yield_pct", precision = 6, scale = 2)
    private BigDecimal expectedYieldPct;
}
