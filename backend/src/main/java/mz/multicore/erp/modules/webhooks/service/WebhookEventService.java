package mz.multicore.erp.modules.webhooks.service;

import mz.multicore.erp.architecture.events.CreditNoteApprovedEvent;
import mz.multicore.erp.architecture.events.OrderStatusChangedEvent;
import mz.multicore.erp.architecture.events.SaleRegisteredEvent;
import mz.multicore.erp.architecture.events.StockAvailabilityChangedEvent;
import mz.multicore.erp.modules.webhooks.model.WebhookEventType;
import mz.multicore.erp.modules.webhooks.model.WebhookSubscription;
import mz.multicore.erp.modules.webhooks.repository.WebhookSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Chooses who receives each webhook event and builds its body (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.2, §4.2.1,
 * WH-02): only <b>active</b> subscriptions of the event's company that chose that type. With nobody interested no body
 * is built; otherwise one body (same {@code id}) goes to each subscription. Sending after commit is WH-03.
 */
@Service
public class WebhookEventService {

    private final WebhookSubscriptionRepository repository;
    private final WebhookDomainEventTranslator translator;
    private final WebhookEventFactory factory;

    public WebhookEventService(WebhookSubscriptionRepository repository, WebhookDomainEventTranslator translator,
                               WebhookEventFactory factory) {
        this.repository = repository;
        this.translator = translator;
        this.factory = factory;
    }

    @Transactional(readOnly = true)
    public List<PreparedWebhookEvent> prepare(SaleRegisteredEvent event) {
        return prepare(event.companyId(), WebhookEventType.INVOICE_ISSUED, () -> translator.invoiceIssued(event));
    }

    @Transactional(readOnly = true)
    public List<PreparedWebhookEvent> prepare(CreditNoteApprovedEvent event) {
        return prepare(event.companyId(), WebhookEventType.CREDIT_NOTE_ISSUED,
                () -> translator.creditNoteIssued(event));
    }

    @Transactional(readOnly = true)
    public List<PreparedWebhookEvent> prepare(OrderStatusChangedEvent event) {
        return prepare(event.companyId(), WebhookEventType.ORDER_STATUS_CHANGED,
                () -> translator.orderStatusChanged(event));
    }

    @Transactional(readOnly = true)
    public List<PreparedWebhookEvent> prepare(StockAvailabilityChangedEvent event) {
        return prepare(event.companyId(), WebhookEventType.STOCK_AVAILABILITY_CHANGED,
                () -> translator.stockAvailabilityChanged(event));
    }

    private List<PreparedWebhookEvent> prepare(Long companyId, WebhookEventType type,
                                               Supplier<Optional<TranslatedWebhookEvent>> fact) {
        if (companyId == null) {
            return List.of();
        }
        List<WebhookSubscription> interested = repository.findByCompanyIdAndActiveTrue(companyId).stream()
                .filter(subscription -> subscription.getEvents().contains(type))
                .toList();
        if (interested.isEmpty()) {
            return List.of();
        }
        return fact.get()
                .map(translated -> factory.build(companyId, translated.occurredAt(), translated.data()))
                .map(payload -> interested.stream()
                        .map(subscription -> new PreparedWebhookEvent(subscription.getId(), subscription.getUrl(),
                                payload))
                        .toList())
                .orElse(List.of());
    }
}
