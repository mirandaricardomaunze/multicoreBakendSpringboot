package mz.multicore.erp.modules.units.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;

import java.math.BigDecimal;

/**
 * Unit of measure (SA-C02). System units have {@code company=null} and are seeded by
 * {@code SystemUnitCatalogSeeder}; each company may add its own package units.
 */
@Entity
@Table(name = "units_of_measure", uniqueConstraints = {
        @UniqueConstraint(name = "uk_unit_company_code", columnNames = {"company_id", "code"})
})
@Getter
@Setter
public class UnitOfMeasure extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** {@code null} for system units. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "symbol", length = 20)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "dimension", nullable = false, length = 20)
    private UnitDimension dimension;

    /** Value of one unit in the dimension reference unit; {@code null} for package units. */
    @Column(name = "dimension_factor", precision = 18, scale = 6)
    private BigDecimal dimensionFactor;

    @Column(name = "decimals", nullable = false)
    private int decimals;

    @Column(name = "system_unit", nullable = false)
    private boolean systemUnit;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
