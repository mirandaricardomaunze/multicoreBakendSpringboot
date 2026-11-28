package mz.multicore.erp.modules.webhooks.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;

import java.time.LocalDateTime;

/**
 * One webhook event to be delivered to one subscription (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.2.2, WH-03). The JSON
 * body is serialized once, after the business commit, and never rebuilt: retries and resends send exactly the same
 * text with the same {@code eventId}.
 */
@Entity
@Table(name = "webhook_deliveries", indexes = {
        @Index(name = "idx_webhook_deliveries_due", columnList = "status, next_attempt_at"),
        @Index(name = "idx_webhook_deliveries_subscription", columnList = "subscription_id, created_at")
})
@Getter
@Setter
public class WebhookDelivery extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private WebhookSubscription subscription;

    @Column(name = "event_id", nullable = false, length = 60)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private WebhookEventType eventType;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WebhookDeliveryStatus status = WebhookDeliveryStatus.PENDING;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "last_http_status")
    private Integer lastHttpStatus;

    @Column(name = "last_duration_ms")
    private Long lastDurationMs;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
}
