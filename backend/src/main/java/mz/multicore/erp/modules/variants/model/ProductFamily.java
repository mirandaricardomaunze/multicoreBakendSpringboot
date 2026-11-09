package mz.multicore.erp.modules.variants.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.company.model.Company;

import java.util.ArrayList;
import java.util.List;

/** A product family with up to two attributes (SA-C04, {@code docs/VARIANTES_ARTIGO_SPEC.md}). */
@Entity
@Table(name = "product_families", uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_family_prefix", columnNames = {"company_id", "sku_prefix"})
})
@Getter
@Setter
public class ProductFamily extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "sku_prefix", nullable = false, length = 20)
    private String skuPrefix;

    @Column(name = "attribute1", nullable = false, length = 40)
    private String attribute1;

    @Column(name = "attribute2", length = 40)
    private String attribute2;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_product_id", nullable = false)
    private Product templateProduct;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "family", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ProductVariant> variants = new ArrayList<>();
}
