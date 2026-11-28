package mz.multicore.erp.modules.webhooks.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Hands the due {@code PENDING} deliveries of active subscriptions to the {@link WebhookDispatcher}
 * (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.3.2, WH-05): the retries, and first attempts whose async dispatch was lost in
 * a restart. At most {@value #BATCH} per round; the conditional claim in {@link WebhookDeliveryService#startAttempt}
 * keeps a delivery from being sent twice.
 */
@Component
@ConditionalOnProperty(name = "webhooks.retry.enabled", havingValue = "true", matchIfMissing = true)
public class WebhookRetryScheduler {

    static final int BATCH = 50;

    private static final Logger log = LoggerFactory.getLogger(WebhookRetryScheduler.class);

    private final WebhookDeliveryService deliveries;
    private final WebhookDispatcher dispatcher;

    public WebhookRetryScheduler(WebhookDeliveryService deliveries, WebhookDispatcher dispatcher) {
        this.deliveries = deliveries;
        this.dispatcher = dispatcher;
    }

    @Scheduled(initialDelayString = "${webhooks.retry.poll-ms:30000}",
            fixedDelayString = "${webhooks.retry.poll-ms:30000}")
    public void dispatchDue() {
        try {
            deliveries.dueDeliveryIds(BATCH).forEach(dispatcher::dispatch);
        } catch (RuntimeException e) {
            log.warn("Agendador de webhooks falhou nesta volta: {}", e.getClass().getSimpleName());
        }
    }
}
