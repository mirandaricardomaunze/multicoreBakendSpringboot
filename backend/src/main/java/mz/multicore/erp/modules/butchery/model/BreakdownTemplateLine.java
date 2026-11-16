package mz.multicore.erp.modules.butchery.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.comercial.model.Product;

import java.math.BigDecimal;

/** One cut expected from a breakdown model, with its share of the carcass weight. */
@Entity
@Table(name = "breakdown_template_lines")
@Getter
@Setter
public class BreakdownTemplateLine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private BreakdownTemplate template;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "expected_yield_pct", nullable = false, precision = 6, scale = 2)
    private BigDecimal expectedYieldPct = BigDecimal.ZERO;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
