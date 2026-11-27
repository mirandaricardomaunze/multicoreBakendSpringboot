package mz.multicore.erp.modules.webhooks.service;

import mz.multicore.erp.modules.webhooks.model.WebhookDelivery;
import mz.multicore.erp.modules.webhooks.model.WebhookDeliveryStatus;
import mz.multicore.erp.modules.webhooks.model.WebhookSubscription;
import mz.multicore.erp.modules.webhooks.repository.WebhookDeliveryRepository;
import mz.multicore.erp.modules.webhooks.repository.WebhookSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/** Records webhook deliveries and their send attempts (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.2.2, §4.3.1). */
@Service
public class WebhookDeliveryService {

    private static final int MAX_ERROR = 500;

    private final WebhookDeliveryRepository deliveries;
    private final WebhookSubscriptionRepository subscriptions;

    public WebhookDeliveryService(WebhookDeliveryRepository deliveries, WebhookSubscriptionRepository subscriptions) {
        this.deliveries = deliveries;
        this.subscriptions = subscriptions;
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
        delivery.setNextAttemptAt(LocalDateTime.now());
        return deliveries.save(delivery).getId();
    }

    /**
     * Reads what the next attempt needs (WH-04); empty when the delivery is gone, no longer {@code PENDING} or its
     * subscription is inactive — then nothing is sent and nothing changes.
     */
    @Transactional(readOnly = true)
    public Optional<WebhookAttempt> startAttempt(Long deliveryId) {
        return deliveries.findById(deliveryId)
                .filter(delivery -> delivery.getStatus() == WebhookDeliveryStatus.PENDING)
                .filter(delivery -> delivery.getSubscription().isActive())
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
            delivery.setDeliveredAt(LocalDateTime.now());
            delivery.setNextAttemptAt(null);
            deliveries.save(delivery);
        });
    }

    /**
     * Failed attempt (WH-04): the delivery stays {@code PENDING} with the attempt recorded; the retry schedule and
     * {@code FAILED} are WH-05.
     */
    @Transactional
    public void recordFailure(Long deliveryId, Integer httpStatus, long durationMs, String error) {
        deliveries.findById(deliveryId).ifPresent(delivery -> {
            countAttempt(delivery, httpStatus, durationMs, error);
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
