package mz.multicore.erp.modules.webhooks.service;

import mz.multicore.erp.modules.webhooks.model.WebhookEventType;

/** One built webhook event: its {@code evt_} id, type and JSON body (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.2.1). */
record WebhookEventPayload(String eventId, WebhookEventType type, String body) {
}
