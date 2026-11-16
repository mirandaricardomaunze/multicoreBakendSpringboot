package mz.multicore.erp.modules.bulkstock.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.company.model.Company;

import java.math.BigDecimal;

/** Product measured in bulk by a company, with its natural-loss tolerance (SA-C17). */
@Entity
@Table(name = "bulk_stock_settings", uniqueConstraints = {
        @UniqueConstraint(name = "uk_bulk_stock_setting", columnNames = {"company_id", "product_id"})
})
@Getter
@Setter
public class BulkStockSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "tolerance_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal tolerancePercent;
}
