package mz.multicore.erp.modules.webhooks.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;

import java.util.EnumSet;
import java.util.Set;

/**
 * Webhook subscription of a company (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.1, WH-01). The URL and the events never
 * change after creation; only {@code active} does. The {@code whsec_} secret is kept encrypted at rest because the
 * HMAC signature (§4.3) needs it in clear.
 */
@Entity
@Table(name = "webhook_subscriptions")
@Getter
@Setter
public class WebhookSubscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Convert(converter = WebhookEventsConverter.class)
    @Column(name = "events", nullable = false, length = 200)
    private Set<WebhookEventType> events = EnumSet.noneOf(WebhookEventType.class);

    @Column(name = "secret_ciphertext", nullable = false, length = 255)
    private String secretCiphertext;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** Consecutive failed deliveries; 20 deactivate the subscription (WH-07). */
    @Column(name = "consecutive_failures", nullable = false)
    private int consecutiveFailures;
}
