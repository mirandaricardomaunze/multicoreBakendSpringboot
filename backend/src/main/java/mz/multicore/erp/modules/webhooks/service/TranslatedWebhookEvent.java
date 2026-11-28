package mz.multicore.erp.modules.webhooks.service;

import java.time.OffsetDateTime;

/** A domain fact translated into its webhook summary, with the instant it happened (WH-02). */
record TranslatedWebhookEvent(OffsetDateTime occurredAt, WebhookEventData data) {
}
