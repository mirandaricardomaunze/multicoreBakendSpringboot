package mz.multicore.erp.modules.webhooks.service;

/**
 * Port that sends a recorded {@code PENDING} delivery outside the business thread (docs/API_PUBLICA_WEBHOOKS_SPEC.md
 * §4.2.2, WH-03). Implementations must return immediately; the HTTP POST and signature are WH-04, retries WH-05.
 */
public interface WebhookDispatcher {

    void dispatch(Long deliveryId);
}
