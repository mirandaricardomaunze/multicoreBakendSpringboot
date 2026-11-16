package mz.multicore.erp.modules.pricing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;

import java.time.LocalDate;

/**
 * Company price list (SA-C03), such as "Revendedor", "Empreiteiro" or "Funcionário". It applies to the
 * clients it is assigned to while it is active and within its validity period.
 */
@Entity
@Table(name = "price_lists", uniqueConstraints = {
        @UniqueConstraint(name = "uk_price_list_company_code", columnNames = {"company_id", "code"})
})
@Getter
@Setter
public class PriceList extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
