package mz.multicore.erp.modules.purchases.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;

import java.time.LocalDateTime;

/** Registo imutável de uma confirmação parcial, usado para replay seguro. */
@Entity
@Table(name = "purchase_order_receipts", uniqueConstraints = @UniqueConstraint(
        name = "uk_purchase_order_receipt_key", columnNames = {"purchase_order_id", "idempotency_key"}))
@Getter
@Setter
public class PurchaseOrderReceipt extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(name = "idempotency_key", nullable = false, length = 60)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", nullable = false, length = 64)
    private String requestFingerprint;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt = LocalDateTime.now();
}
