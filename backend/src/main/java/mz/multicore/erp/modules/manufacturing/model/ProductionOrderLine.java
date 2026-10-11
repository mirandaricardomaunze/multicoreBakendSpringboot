package mz.multicore.erp.modules.manufacturing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.modules.comercial.model.Product;

import java.math.BigDecimal;

/**
 * Linha de componente consumido dentro de uma Ordem de Produção.
 */
@Entity
@Table(name = "production_order_lines")
@Getter
@Setter
public class ProductionOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_order_id", nullable = false)
    private ProductionOrder productionOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_product_id", nullable = false)
    private Product componentProduct;

    @Column(name = "planned_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal plannedQuantity;

    @Column(name = "consumed_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal consumedQuantity = BigDecimal.ZERO;

    @Column(name = "unit_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Column(name = "total_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;
}
