package mz.multicore.erp.modules.inventory.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;

@Entity
@Table(name = "stock_movement_operations", uniqueConstraints =
        @UniqueConstraint(name = "uk_stock_movement_operation_company_key", columnNames = {"company_id", "idempotency_key"}))
@Getter
@Setter
public class StockMovementOperation extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "idempotency_key", nullable = false, length = 60)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", nullable = false, length = 64)
    private String requestFingerprint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_movement_id")
    private StockMovement stockMovement;
}
