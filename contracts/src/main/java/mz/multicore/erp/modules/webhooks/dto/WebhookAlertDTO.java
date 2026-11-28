package mz.multicore.erp.modules.webhooks.dto;

import java.time.LocalDateTime;

/** Alert to the ADMIN: a subscription switched off after 20 failed deliveries in a row (§4.3.4, WH-07). */
public record WebhookAlertDTO(Long subscriptionId, String url, int consecutiveFailures, LocalDateTime disabledAt,
                              String message) {
}
