package mz.multicore.erp.modules.assets.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Registo de depreciação mensal de um activo fixo.
 */
@Entity
@Table(name = "asset_depreciation_records", uniqueConstraints = @UniqueConstraint(
        name = "uk_asset_depreciation_period", columnNames = {"company_id", "asset_id", "period_year", "period_month"}))
@Getter
@Setter
public class AssetDepreciationRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "period_year", nullable = false)
    private int periodYear;

    @Column(name = "period_month", nullable = false)
    private int periodMonth;

    @Column(name = "calculation_date", nullable = false)
    private LocalDate calculationDate;

    @Column(name = "opening_book_value", nullable = false, precision = 14, scale = 2)
    private BigDecimal openingBookValue;

    @Column(name = "depreciation_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal depreciationAmount;

    @Column(name = "closing_book_value", nullable = false, precision = 14, scale = 2)
    private BigDecimal closingBookValue;

    @Column(name = "accumulated_depreciation", nullable = false, precision = 14, scale = 2)
    private BigDecimal accumulatedDepreciation;

    @Column(name = "journal_entry_id")
    private Long journalEntryId;
}
