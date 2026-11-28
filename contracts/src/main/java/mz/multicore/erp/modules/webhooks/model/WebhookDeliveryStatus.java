package mz.multicore.erp.modules.webhooks.model;

/** State of one webhook delivery (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.2.2, §4.3). */
public enum WebhookDeliveryStatus {
    /** Recorded after the commit, waiting to be sent or retried. */
    PENDING,
    /** The receiver answered 2xx. */
    DELIVERED,
    /** All attempts failed (WH-05); may be resent manually (WH-06). */
    FAILED
}
