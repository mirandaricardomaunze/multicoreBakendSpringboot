package mz.multicore.erp.modules.webhooks.dto;

import mz.multicore.erp.modules.webhooks.model.WebhookEventType;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * A webhook subscription as listed to the ADMIN: never carries the secret (§4.1.1). {@code autoDisabledAt} is set
 * only when it was switched off after 20 failed deliveries in a row (§4.3.4).
 */
public record WebhookSubscriptionDTO(Long id, String url, Set<WebhookEventType> events, boolean active,
                                     int consecutiveFailures, LocalDateTime createdAt, String createdBy,
                                     LocalDateTime autoDisabledAt) {
}
