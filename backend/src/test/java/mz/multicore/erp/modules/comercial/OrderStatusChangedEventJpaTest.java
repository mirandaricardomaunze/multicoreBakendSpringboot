package mz.multicore.erp.modules.comercial;

import jakarta.persistence.EntityManager;
import mz.multicore.erp.architecture.events.OrderStatusChangedEvent;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.OrderStatus;
import mz.multicore.erp.modules.company.model.Company;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Harness WH-02 (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.2.1): through real Hibernate, every status change of a customer
 * order that reaches the database publishes one {@link OrderStatusChangedEvent}; creating the order and writing the
 * same status again publish nothing, whatever service changed it.
 */
@DataJpaTest(showSql = false, properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "logging.level.org.hibernate.SQL=OFF",
        "logging.level.org.springframework=WARN"
})
@RecordApplicationEvents
class OrderStatusChangedEventJpaTest {

    @Autowired private EntityManager entityManager;
    @Autowired private ApplicationEvents events;

    @Test
    void creatingAnOrderIsNotAStatusChange() {
        persistOrder("EC-2026/1", OrderStatus.PENDING_APPROVAL);

        assertThat(published()).isEmpty();
    }

    @Test
    void eachRealChangeOfTheLoadedOrderPublishesPreviousAndNewStatus() {
        Order order = persistOrder("EC-2026/2", OrderStatus.PENDING);
        entityManager.clear();

        Order loaded = entityManager.find(Order.class, order.getId());
        loaded.setStatus(OrderStatus.BILLED.name());
        entityManager.flush();

        List<OrderStatusChangedEvent> published = published();
        assertThat(published).hasSize(1);
        OrderStatusChangedEvent event = published.get(0);
        assertThat(event.companyId()).isEqualTo(order.getCompany().getId());
        assertThat(event.orderId()).isEqualTo(order.getId());
        assertThat(event.orderNumber()).isEqualTo("EC-2026/2");
        assertThat(event.previousStatus()).isEqualTo("PENDING");
        assertThat(event.status()).isEqualTo("BILLED");
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    void changeAfterCreationInTheSameTransactionAndSuccessiveChangesAreEachPublishedOnce() {
        Order order = persistOrder("EC-2026/3", OrderStatus.PENDING);
        order.setStatus(OrderStatus.GUIDE_PENDING.name());
        entityManager.flush();
        order.setStatus(OrderStatus.CANCELLED.name());
        entityManager.flush();
        entityManager.flush();

        assertThat(published()).extracting(OrderStatusChangedEvent::previousStatus, OrderStatusChangedEvent::status)
                .containsExactly(tuple("PENDING", "GUIDE_PENDING"),
                        tuple("GUIDE_PENDING", "CANCELLED"));
    }

    @Test
    void updatingOtherFieldsOrWritingTheSameStatusPublishesNothing() {
        Order order = persistOrder("EC-2026/4", OrderStatus.PENDING);
        entityManager.clear();

        Order loaded = entityManager.find(Order.class, order.getId());
        loaded.setNotes("Entregar de manhã");
        loaded.setStatus(OrderStatus.PENDING.name());
        entityManager.flush();

        assertThat(published()).isEmpty();
    }

    private List<OrderStatusChangedEvent> published() {
        return events.stream(OrderStatusChangedEvent.class).toList();
    }

    private Order persistOrder(String number, OrderStatus status) {
        Company company = new Company();
        company.setName("Loja " + number);
        company.setTaxId("company-" + number);
        entityManager.persist(company);
        Client client = new Client();
        client.setName("Padaria Central");
        client.setTaxId("client-" + number);
        client.setEmail("padaria." + number.replace('/', '.') + "@example.test");
        entityManager.persist(client);
        Order order = new Order();
        order.setCompany(company);
        order.setClient(client);
        order.setOrderNumber(number);
        order.setStatus(status.name());
        entityManager.persist(order);
        entityManager.flush();
        return order;
    }
}
