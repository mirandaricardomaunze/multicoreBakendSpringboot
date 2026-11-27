package mz.multicore.erp.modules.webhooks.dto;

import mz.multicore.erp.modules.webhooks.model.WebhookDeliveryStatus;

import java.time.LocalDateTime;

/**
 * One line of a subscription's delivery log, with the data of its last attempt (docs/API_PUBLICA_WEBHOOKS_SPEC.md
 * §4.3.3): never the body (it carries the customer's name/NUIT) nor the secret. {@code eventType} is the wire code.
 */
public record WebhookDeliveryDTO(Long id, String eventId, String eventType, WebhookDeliveryStatus status,
                                 int attempts, Integer lastHttpStatus, Long lastDurationMs, String lastError,
                                 LocalDateTime nextAttemptAt, LocalDateTime createdAt, LocalDateTime deliveredAt) {
}
