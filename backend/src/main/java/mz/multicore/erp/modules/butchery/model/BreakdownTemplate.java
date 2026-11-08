package mz.multicore.erp.modules.butchery.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.company.model.Company;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Breakdown model of a carcass (for example "Boi inteiro"): the cuts it should give and the loss accepted. */
@Entity
@Table(name = "breakdown_templates")
@Getter
@Setter
public class BreakdownTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_product_id", nullable = false)
    private Product sourceProduct;

    @Column(name = "loss_tolerance_pct", nullable = false, precision = 6, scale = 2)
    private BigDecimal lossTolerancePct = BigDecimal.ZERO;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<BreakdownTemplateLine> lines = new ArrayList<>();
}
