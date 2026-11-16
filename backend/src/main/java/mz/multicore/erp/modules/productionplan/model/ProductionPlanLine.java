package mz.multicore.erp.modules.productionplan.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.manufacturing.model.BillOfMaterials;

import java.math.BigDecimal;

/** One recipe in the day plan. */
@Entity
@Table(name = "production_plan_lines")
@Getter
@Setter
public class ProductionPlanLine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private ProductionPlan plan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bom_id", nullable = false)
    private BillOfMaterials bom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "planned_qty", nullable = false, precision = 14, scale = 3)
    private BigDecimal plannedQty = BigDecimal.ZERO;

    @Column(name = "produced_qty", nullable = false, precision = 14, scale = 3)
    private BigDecimal producedQty = BigDecimal.ZERO;

    @Column(name = "leftover_qty", nullable = false, precision = 14, scale = 3)
    private BigDecimal leftoverQty = BigDecimal.ZERO;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
