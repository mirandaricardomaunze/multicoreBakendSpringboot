package mz.multicore.erp.modules.webhooks.model;

import java.util.Arrays;
import java.util.Optional;

/** Events a webhook subscription may choose (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.2), with their wire code. */
public enum WebhookEventType {
    INVOICE_ISSUED("invoice.issued"),
    CREDIT_NOTE_ISSUED("credit_note.issued"),
    ORDER_STATUS_CHANGED("order.status_changed"),
    STOCK_AVAILABILITY_CHANGED("stock.availability_changed");

    private final String code;

    WebhookEventType(String code) {
        this.code = code;
    }

    /** Code sent in {@code type} and in the {@code X-Multicore-Event} header, e.g. {@code invoice.issued}. */
    public String code() {
        return code;
    }

    /** Accepts the wire code or the enum name, ignoring surrounding spaces. */
    public static Optional<WebhookEventType> fromCode(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String value = raw.trim();
        return Arrays.stream(values())
                .filter(type -> type.code.equals(value) || type.name().equals(value))
                .findFirst();
    }
}
