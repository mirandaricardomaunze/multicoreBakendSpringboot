package mz.multicore.erp.modules.webhooks.service;

/**
 * What one send attempt needs, read from a {@code PENDING} delivery of an active subscription (§4.3.1, WH-04). The
 * secret stays encrypted here; only the sender opens it, in memory, to sign.
 */
record WebhookAttempt(Long deliveryId, String url, String eventCode, String body, String secretCiphertext) {

    @Override
    public String toString() {
        return "WebhookAttempt[deliveryId=" + deliveryId + ", eventCode=" + eventCode + "]";
    }
}
