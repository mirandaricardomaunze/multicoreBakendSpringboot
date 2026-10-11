package mz.multicore.erp.modules.assets.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entidade JPA representativa de um Activo Fixo Patrimonial.
 */
@Entity
@Table(name = "fixed_assets", uniqueConstraints = @UniqueConstraint(
        name = "uk_fixed_assets_code", columnNames = {"company_id", "asset_code"}))
@Getter
@Setter
public class Asset extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "asset_code", nullable = false, length = 32)
    private String assetCode;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    private AssetCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AssetStatus status = AssetStatus.ACTIVE;

    @Column(name = "acquisition_date", nullable = false)
    private LocalDate acquisitionDate;

    @Column(name = "acquisition_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal acquisitionCost;

    @Column(name = "residual_value", nullable = false, precision = 14, scale = 2)
    private BigDecimal residualValue = BigDecimal.ZERO;

    @Column(name = "annual_depreciation_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal annualDepreciationRate;

    @Column(name = "accumulated_depreciation", nullable = false, precision = 14, scale = 2)
    private BigDecimal accumulatedDepreciation = BigDecimal.ZERO;

    @Column(name = "net_book_value", nullable = false, precision = 14, scale = 2)
    private BigDecimal netBookValue;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "location", length = 120)
    private String location;

    @Column(name = "assigned_employee", length = 150)
    private String assignedEmployee;

    @Column(name = "notes", length = 500)
    private String notes;
}
