package mz.multicore.erp.modules.webhooks.service;

import mz.multicore.erp.modules.webhooks.model.WebhookDelivery;
import mz.multicore.erp.modules.webhooks.model.WebhookDeliveryStatus;
import mz.multicore.erp.modules.webhooks.model.WebhookSubscription;
import mz.multicore.erp.modules.webhooks.repository.WebhookDeliveryRepository;
import mz.multicore.erp.modules.webhooks.repository.WebhookSubscriptionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Records webhook deliveries and their send attempts, retry schedule included (docs/API_PUBLICA_WEBHOOKS_SPEC.md
 * §4.2.2, §4.3.1, §4.3.2).
 */
@Service
public class WebhookDeliveryService {

    private static final int MAX_ERROR = 500;

    private final WebhookDeliveryRepository deliveries;
    private final WebhookSubscriptionRepository subscriptions;
    private final WebhookRetryPolicy retryPolicy;

    public WebhookDeliveryService(WebhookDeliveryRepository deliveries, WebhookSubscriptionRepository subscriptions,
                                  WebhookRetryPolicy retryPolicy) {
        this.deliveries = deliveries;
        this.subscriptions = subscriptions;
        this.retryPolicy = retryPolicy;
    }

    /**
     * Records one {@code PENDING} delivery with the already serialized body, in a transaction of its own: the
     * business transaction has committed already, and one subscription failing must not stop the others.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long registerPending(PreparedWebhookEvent prepared) {
        WebhookSubscription subscription = subscriptions.getReferenceById(prepared.subscriptionId());
        WebhookDelivery delivery = new WebhookDelivery();
        delivery.setSubscription(subscription);
        delivery.setCompany(subscription.getCompany());
        delivery.setEventId(prepared.payload().eventId());
        delivery.setEventType(prepared.payload().type());
        delivery.setBody(prepared.payload().body());
        delivery.setStatus(WebhookDeliveryStatus.PENDING);
        delivery.setAttempts(0);
        delivery.setNextAttemptAt(retryPolicy.now());
        return deliveries.save(delivery).getId();
    }

    /** Ids of the due {@code PENDING} deliveries of active subscriptions, oldest due first (WH-05). */
    @Transactional(readOnly = true)
    public List<Long> dueDeliveryIds(int limit) {
        return deliveries.findDueIds(WebhookDeliveryStatus.PENDING, retryPolicy.now(), PageRequest.of(0, limit));
    }

    /**
     * Claims the next attempt and reads what it needs (WH-04, WH-05); empty when the delivery is gone, no longer
     * {@code PENDING}, not yet due, already claimed by another thread, or its subscription is inactive — then nothing
     * is sent and nothing changes. The claim reserves the attempt for {@link WebhookRetryPolicy#CLAIM_LEASE}.
     */
    @Transactional
    public Optional<WebhookAttempt> startAttempt(Long deliveryId) {
        LocalDateTime now = retryPolicy.now();
        return deliveries.findById(deliveryId)
                .filter(delivery -> delivery.getStatus() == WebhookDeliveryStatus.PENDING)
                .filter(delivery -> delivery.getSubscription().isActive())
                .filter(delivery -> deliveries.claim(delivery.getId(), WebhookDeliveryStatus.PENDING, now,
                        retryPolicy.claimLeaseUntil(now)) == 1)
                .map(delivery -> new WebhookAttempt(delivery.getId(), delivery.getSubscription().getUrl(),
                        delivery.getEventType().code(), delivery.getBody(),
                        delivery.getSubscription().getSecretCiphertext()));
    }

    /** 2xx: the delivery is done (WH-04). */
    @Transactional
    public void recordDelivered(Long deliveryId, int httpStatus, long durationMs) {
        deliveries.findById(deliveryId).ifPresent(delivery -> {
            countAttempt(delivery, httpStatus, durationMs, null);
            delivery.setStatus(WebhookDeliveryStatus.DELIVERED);
            delivery.setDeliveredAt(retryPolicy.now());
            delivery.setNextAttemptAt(null);
            deliveries.save(delivery);
        });
    }

    /**
     * Failed attempt (WH-04): the attempt is recorded and the next one scheduled from the end of this one; after the
     * last allowed attempt the delivery becomes {@code FAILED} (WH-05).
     */
    @Transactional
    public void recordFailure(Long deliveryId, Integer httpStatus, long durationMs, String error) {
        deliveries.findById(deliveryId).ifPresent(delivery -> {
            countAttempt(delivery, httpStatus, durationMs, error);
            Optional<LocalDateTime> next = retryPolicy.nextAttemptAt(delivery.getAttempts(), retryPolicy.now());
            delivery.setNextAttemptAt(next.orElse(null));
            if (next.isEmpty()) {
                delivery.setStatus(WebhookDeliveryStatus.FAILED);
            }
            deliveries.save(delivery);
        });
    }

    private static void countAttempt(WebhookDelivery delivery, Integer httpStatus, long durationMs, String error) {
        delivery.setAttempts(delivery.getAttempts() + 1);
        delivery.setLastHttpStatus(httpStatus);
        delivery.setLastDurationMs(durationMs);
        delivery.setLastError(error == null || error.length() <= MAX_ERROR ? error : error.substring(0, MAX_ERROR));
    }
}
