package mz.multicore.erp.modules.webhooks.service;

/**
 * A {@code FAILED} delivery was put back to {@code PENDING} by the ADMIN (§4.3.3, WH-06); dispatched only after the
 * resend commits, by {@link WebhookEventListener}.
 */
public record WebhookResendRequested(Long deliveryId) {
}
