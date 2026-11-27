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

/** Records webhook deliveries (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.2.2, WH-03). */
@Service
public class WebhookDeliveryService {

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
}
