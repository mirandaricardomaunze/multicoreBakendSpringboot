package mz.multicore.erp.modules.webhooks.service;

import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.webhooks.model.WebhookSubscription;
import mz.multicore.erp.modules.webhooks.repository.WebhookSubscriptionRepository;
import org.springframework.stereotype.Component;

/**
 * Counts failed deliveries in a row per subscription and switches it off at the 20th
 * (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.3.4, WH-07). Runs inside the transaction that records the delivery outcome;
 * every change is an atomic database update, so parallel sends never lose counts nor deactivate twice.
 */
@Component
public class WebhookFailureStreak {

    /** Failed deliveries in a row that switch a subscription off. */
    static final int THRESHOLD = 20;
    static final String SYSTEM_USER = "sistema";

    private final WebhookSubscriptionRepository subscriptions;
    private final WebhookRetryPolicy retryPolicy;
    private final AuditLogService auditLogService;

    public WebhookFailureStreak(WebhookSubscriptionRepository subscriptions, WebhookRetryPolicy retryPolicy,
                                AuditLogService auditLogService) {
        this.subscriptions = subscriptions;
        this.retryPolicy = retryPolicy;
        this.auditLogService = auditLogService;
    }

    /** One delivery of the subscription arrived: the streak ends. */
    void delivered(WebhookSubscription subscription) {
        subscriptions.resetConsecutiveFailures(subscription.getId());
    }

    /** One delivery of the subscription became {@code FAILED}: +1, and at {@value #THRESHOLD} it is switched off. */
    void failed(WebhookSubscription subscription) {
        Long id = subscription.getId();
        subscriptions.incrementConsecutiveFailures(id);
        if (subscriptions.autoDisable(id, THRESHOLD, retryPolicy.now()) == 1) {
            int failures = subscriptions.consecutiveFailures(id);
            auditLogService.logEvent(SYSTEM_USER, subscription.getCompany().getId(), "WEBHOOK_AUTO_DEACTIVATE",
                    "Webhook " + subscription.getUrl() + " desactivado após " + failures
                            + " entregas falhadas seguidas");
        }
    }
}
