package mz.multicore.erp.modules.webhooks.service;

/**
 * A webhook event ready to be delivered to one subscription (WH-02); the delivery itself is WH-03/04. The same
 * {@link WebhookEventPayload} (same {@code id}) goes to every interested subscription.
 */
record PreparedWebhookEvent(Long subscriptionId, String url, WebhookEventPayload payload) {
}
