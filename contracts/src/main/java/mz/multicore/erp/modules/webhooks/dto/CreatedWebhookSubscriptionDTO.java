package mz.multicore.erp.modules.webhooks.dto;

/** Result of creating a subscription: the only time the {@code whsec_} secret leaves the server (§4.1.1). */
public record CreatedWebhookSubscriptionDTO(WebhookSubscriptionDTO subscription, String secret) {
}
