package mz.multicore.erp.gui.integrations;

import mz.multicore.erp.modules.webhooks.dto.WebhookDeliveryDTO;
import mz.multicore.erp.modules.webhooks.dto.WebhookSubscriptionDTO;
import mz.multicore.erp.modules.webhooks.model.WebhookDeliveryStatus;
import mz.multicore.erp.modules.webhooks.model.WebhookEventType;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** PT-MZ texts of a webhook and of its deliveries as the screen shows them (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.4.2). */
final class WebhookLabels {

    static final String NONE = "—";

    private WebhookLabels() {
    }

    static String event(WebhookEventType type) {
        return switch (type) {
            case INVOICE_ISSUED -> "Factura emitida";
            case CREDIT_NOTE_ISSUED -> "Nota de crédito emitida";
            case ORDER_STATUS_CHANGED -> "Estado da encomenda";
            case STOCK_AVAILABILITY_CHANGED -> "Disponibilidade de stock";
        };
    }

    /** Label of a wire code ({@code invoice.issued}); an unknown code is shown as it comes. */
    static String eventCode(String code) {
        return WebhookEventType.fromCode(code).map(WebhookLabels::event).orElse(code == null ? "" : code);
    }

    /** Labels in the enum order, so every row reads the same way. */
    static String events(Set<WebhookEventType> events) {
        if (events == null) return "";
        return Arrays.stream(WebhookEventType.values()).filter(events::contains)
                .map(WebhookLabels::event).collect(Collectors.joining(", "));
    }

    static String status(WebhookDeliveryStatus status) {
        if (status == null) return "";
        return switch (status) {
            case PENDING -> "Pendente";
            case DELIVERED -> "Entregue";
            case FAILED -> "Falhada";
        };
    }

    static String created(WebhookSubscriptionDTO subscription) {
        String by = subscription.createdBy();
        return ApiKeyLabels.dateTime(subscription.createdAt()) + (by == null || by.isBlank() ? "" : " por " + by);
    }

    static String state(WebhookSubscriptionDTO subscription) {
        if (subscription.active()) return "Activa";
        if (subscription.autoDisabledAt() != null) {
            return "Desactivada automaticamente em " + ApiKeyLabels.dateTime(subscription.autoDisabledAt());
        }
        return "Inactiva";
    }

    static Object[] deliveryRow(WebhookDeliveryDTO delivery) {
        return new Object[]{eventCode(delivery.eventType()), status(delivery.status()), delivery.attempts(),
                delivery.lastHttpStatus() == null ? NONE : String.valueOf(delivery.lastHttpStatus()),
                delivery.lastDurationMs() == null ? NONE : delivery.lastDurationMs() + " ms",
                delivery.lastError() == null ? "" : delivery.lastError(),
                ApiKeyLabels.dateTime(delivery.nextAttemptAt()), ApiKeyLabels.dateTime(delivery.createdAt()),
                ApiKeyLabels.dateTime(delivery.deliveredAt())};
    }
}
