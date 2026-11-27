package mz.multicore.erp.modules.webhooks.service;

import mz.multicore.erp.architecture.events.CreditNoteApprovedEvent;
import mz.multicore.erp.architecture.events.OrderStatusChangedEvent;
import mz.multicore.erp.architecture.events.SaleRegisteredEvent;
import mz.multicore.erp.architecture.events.StockAvailabilityChangedEvent;
import mz.multicore.erp.modules.webhooks.model.WebhookEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.function.Supplier;

/**
 * Sends webhooks only after the business transaction commits (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.2.2, WH-03):
 * rollback → nothing. Each interested subscription gets one {@code PENDING} delivery, then the dispatcher sends it
 * off the business thread. Any error on the webhook path is caught and logged (event type and company only, never
 * secret nor body): the operation is already confirmed and the other subscriptions go on.
 */
@Component
public class WebhookEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebhookEventListener.class);

    private final WebhookEventService events;
    private final WebhookDeliveryService deliveries;
    private final WebhookDispatcher dispatcher;

    public WebhookEventListener(WebhookEventService events, WebhookDeliveryService deliveries,
                                WebhookDispatcher dispatcher) {
        this.events = events;
        this.deliveries = deliveries;
        this.dispatcher = dispatcher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSale(SaleRegisteredEvent event) {
        deliver(WebhookEventType.INVOICE_ISSUED, event.companyId(), () -> events.prepare(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCreditNote(CreditNoteApprovedEvent event) {
        deliver(WebhookEventType.CREDIT_NOTE_ISSUED, event.companyId(), () -> events.prepare(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderStatus(OrderStatusChangedEvent event) {
        deliver(WebhookEventType.ORDER_STATUS_CHANGED, event.companyId(), () -> events.prepare(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onStockAvailability(StockAvailabilityChangedEvent event) {
        deliver(WebhookEventType.STOCK_AVAILABILITY_CHANGED, event.companyId(), () -> events.prepare(event));
    }

    private void deliver(WebhookEventType type, Long companyId, Supplier<List<PreparedWebhookEvent>> prepare) {
        List<PreparedWebhookEvent> prepared;
        try {
            prepared = prepare.get();
        } catch (RuntimeException e) {
            warn(type, companyId, null, e);
            return;
        }
        for (PreparedWebhookEvent event : prepared) {
            try {
                dispatcher.dispatch(deliveries.registerPending(event));
            } catch (RuntimeException e) {
                warn(type, companyId, event.subscriptionId(), e);
            }
        }
    }

    private static void warn(WebhookEventType type, Long companyId, Long subscriptionId, RuntimeException e) {
        log.warn("Webhook {} da empresa {} (subscrição {}) falhou ao registar ou despachar: {}", type.code(), companyId,
                subscriptionId == null ? "-" : subscriptionId, e.getClass().getSimpleName());
    }
}
