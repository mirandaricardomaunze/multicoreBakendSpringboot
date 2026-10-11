package mz.multicore.erp.modules.manufacturing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.modules.comercial.model.Product;

import java.math.BigDecimal;

@Entity
@Table(name = "manufacturing_bom_items")
@Getter
@Setter
public class BomItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bom_id", nullable = false)
    private BillOfMaterials bom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_product_id", nullable = false)
    private Product rawMaterialProduct;

    @Column(name = "quantity_required", nullable = false, precision = 15, scale = 4)
    private BigDecimal requiredQuantity;

    @Column(name = "unit_of_measure", length = 30)
    private String unitOfMeasure;

    @Column(name = "scrap_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal scrapPercentage = BigDecimal.ZERO;
}
