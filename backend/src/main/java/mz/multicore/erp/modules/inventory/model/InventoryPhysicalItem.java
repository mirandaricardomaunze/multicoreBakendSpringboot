package mz.multicore.erp.modules.inventory.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.comercial.model.Product;

import java.math.BigDecimal;

@Entity
@Table(name = "inventory_physical_items")
@Getter
@Setter
@NoArgsConstructor
public class InventoryPhysicalItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private InventoryPhysicalSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "product_code", length = 100)
    private String productCode;

    @Column(name = "product_name", length = 255)
    private String productName;

    @Column(name = "barcode", length = 100)
    private String barcode;

    @Column(name = "expected_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal expectedQuantity = BigDecimal.ZERO;

    @Column(name = "counted_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal countedQuantity = BigDecimal.ZERO;

    @Column(name = "difference", nullable = false, precision = 19, scale = 4)
    private BigDecimal difference = BigDecimal.ZERO;

    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Column(name = "financial_impact", nullable = false, precision = 19, scale = 4)
    private BigDecimal financialImpact = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public void recalculate() {
        if (countedQuantity == null) countedQuantity = BigDecimal.ZERO;
        if (expectedQuantity == null) expectedQuantity = BigDecimal.ZERO;
        if (unitCost == null) unitCost = BigDecimal.ZERO;
        this.difference = countedQuantity.subtract(expectedQuantity);
        this.financialImpact = difference.multiply(unitCost);
    }
}
