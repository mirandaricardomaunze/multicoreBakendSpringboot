package mz.multicore.erp.modules.webhooks.dto;

import java.util.List;

/**
 * ADMIN request to subscribe a URL to webhook events (§4.1.1). Events travel as their wire codes
 * ({@code invoice.issued}, ...) so an unknown one is answered with a PT-MZ message instead of a JSON error.
 */
public record CreateWebhookSubscriptionRequest(String url, List<String> events) {
}
