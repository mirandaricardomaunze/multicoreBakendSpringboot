package mz.multicore.erp.modules.comercial.service;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import mz.multicore.erp.architecture.events.OrderStatusChangedEvent;
import mz.multicore.erp.modules.comercial.model.Order;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Publishes {@link OrderStatusChangedEvent} whenever a customer order's status really changes in the database
 * (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.2.1, WH-02). A single JPA listener on {@link Order} instead of one call per
 * {@code setStatus}: every path (lifecycle, approval, delivery guides, picking) is covered, including future ones.
 * Creating an order is not a status change; the same status written again publishes nothing. The event is published
 * inside the originating transaction, so after-commit listeners (WH-03) only see committed facts.
 */
@Component
public class OrderStatusChangePublisher {

    private final ApplicationEventPublisher eventPublisher;

    public OrderStatusChangePublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @PostLoad
    @PostPersist
    public void remember(Order order) {
        order.rememberPersistedStatus();
    }

    @PostUpdate
    public void publishIfChanged(Order order) {
        order.takePersistedStatusChange().ifPresent(previous -> eventPublisher.publishEvent(
                new OrderStatusChangedEvent(order.getCompany() == null ? null : order.getCompany().getId(),
                        order.getId(), order.getOrderNumber(), previous, order.getStatus(), OffsetDateTime.now())));
    }
}
