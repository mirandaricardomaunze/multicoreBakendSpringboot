package mz.multicore.erp.modules.manufacturing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.company.model.Company;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "manufacturing_boms", uniqueConstraints = @UniqueConstraint(
        name = "uk_company_bom_code", columnNames = {"company_id", "bom_code"}))
@Getter
@Setter
public class BillOfMaterials extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product finishedProduct;

    @Column(name = "bom_code", nullable = false, length = 50)
    private String bomCode;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "yield_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal baseQuantity = BigDecimal.ONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "bom_type", length = 30)
    private BomType bomType = BomType.STANDARD;

    @Column(name = "estimated_labor_cost", precision = 15, scale = 2)
    private BigDecimal estimatedLaborCost = BigDecimal.ZERO;

    @Column(name = "estimated_overhead_cost", precision = 15, scale = 2)
    private BigDecimal estimatedOverheadCost = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "bom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BomItem> items = new ArrayList<>();

    public void addItem(BomItem item) {
        items.add(item);
        item.setBom(this);
    }
}
