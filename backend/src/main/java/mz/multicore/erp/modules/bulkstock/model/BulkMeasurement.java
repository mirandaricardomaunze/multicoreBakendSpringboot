package mz.multicore.erp.modules.bulkstock.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.inventory.model.Warehouse;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A measured bulk pile and its comparison with the system stock (SA-C17). The recorded difference is the
 * physical discrepancy at measurement time; approving it later posts exactly that difference.
 */
@Entity
@Table(name = "bulk_measurements", indexes = {
        @Index(name = "idx_bulk_measurement_company_status", columnList = "company_id, status")
})
@Getter
@Setter
public class BulkMeasurement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private BulkMeasurementMethod method;

    @Column(name = "dimension1", precision = 12, scale = 3)
    private BigDecimal dimension1;

    @Column(name = "dimension2", precision = 12, scale = 3)
    private BigDecimal dimension2;

    @Column(name = "dimension3", precision = 12, scale = 3)
    private BigDecimal dimension3;

    @Column(name = "volume_m3", precision = 18, scale = 3)
    private BigDecimal volumeM3;

    @Column(name = "measured_quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal measuredQuantity;

    @Column(name = "pending_pickup", nullable = false, precision = 18, scale = 3)
    private BigDecimal pendingPickup;

    @Column(name = "system_quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal systemQuantity;

    @Column(name = "difference", nullable = false, precision = 18, scale = 3)
    private BigDecimal difference;

    @Column(name = "difference_percent", nullable = false, precision = 9, scale = 2)
    private BigDecimal differencePercent;

    @Column(name = "tolerance_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal tolerancePercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BulkMeasurementStatus status;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "stock_movement_id")
    private Long stockMovementId;
}
