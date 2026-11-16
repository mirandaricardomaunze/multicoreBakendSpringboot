package mz.multicore.erp.modules.returnables.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.company.model.Company;

/** A product the company lends and expects back (SA-C12, {@code docs/VASILHAME_RETORNAVEIS_SPEC.md}). */
@Entity
@Table(name = "returnable_items", uniqueConstraints = {
        @UniqueConstraint(name = "uk_returnable_item_product", columnNames = {"company_id", "product_id"})
})
@Getter
@Setter
public class ReturnableItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
