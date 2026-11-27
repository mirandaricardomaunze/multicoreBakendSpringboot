package mz.multicore.erp.modules.webhooks.service;

import mz.multicore.erp.modules.audit.service.AuditLogService;

import jakarta.persistence.EntityManager;
import mz.multicore.erp.architecture.events.OrderStatusChangedEvent;
import mz.multicore.erp.architecture.events.StockAvailabilityChangedEvent;
import mz.multicore.erp.modules.comercial.service.InvoiceLookup;
import mz.multicore.erp.modules.comercial.service.InvoiceNoteLookup;
import mz.multicore.erp.modules.comercial.service.OrderLookup;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.webhooks.model.WebhookDelivery;
import mz.multicore.erp.modules.webhooks.model.WebhookDeliveryStatus;
import mz.multicore.erp.modules.webhooks.model.WebhookEventType;
import mz.multicore.erp.modules.webhooks.model.WebhookSubscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Harness WH-03 (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.2.2): through real Hibernate and real commits, a webhook event is
 * recorded only after the business commit — one {@code PENDING} delivery per interested subscription with the same
 * {@code event_id} and the body already serialized, then dispatched — while a rollback records and dispatches
 * nothing and any failure on the webhook path never reaches the caller nor undoes the operation.
 */
@DataJpaTest(showSql = false, properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "logging.level.org.hibernate.SQL=OFF",
        "logging.level.org.springframework=WARN"
})
@Import({WebhookEventListener.class, WebhookEventService.class, WebhookDomainEventTranslator.class,
        WebhookEventFactory.class, WebhookDeliveryService.class, WebhookRetryPolicy.class,
        WebhookAfterCommitJpaTest.Doubles.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class WebhookAfterCommitJpaTest {

    private static final AtomicInteger SEQ = new AtomicInteger();
    private static final OffsetDateTime AT = OffsetDateTime.parse("2026-11-28T09:00:00+02:00");

    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ApplicationEventPublisher publisher;
    @Autowired private RecordingDispatcher dispatcher;
    @Autowired private OrderLookup orderLookup;

    @BeforeEach
    void setUp() {
        dispatcher.reset();
        reset(orderLookup);
    }

    @Test
    void afterTheCommitEachInterestedSubscriptionGetsOnePendingDeliveryWithTheSameEventAndIsDispatched() {
        Long companyId = company();
        Long first = subscribe(companyId, true, EnumSet.of(WebhookEventType.STOCK_AVAILABILITY_CHANGED));
        Long second = subscribe(companyId, true, EnumSet.allOf(WebhookEventType.class));
        subscribe(companyId, true, EnumSet.of(WebhookEventType.INVOICE_ISSUED));
        subscribe(companyId, false, EnumSet.of(WebhookEventType.STOCK_AVAILABILITY_CHANGED));

        AtomicBoolean businessDone = new AtomicBoolean();
        List<Boolean> afterBusiness = new CopyOnWriteArrayList<>();
        dispatcher.onDispatch = id -> afterBusiness.add(businessDone.get());
        inTransaction(em -> {
            publisher.publishEvent(outOfStock(companyId));
            assertThat(deliveries(companyId)).as("nada antes do commit").isEmpty();
            businessDone.set(true);
        });

        List<WebhookDelivery> recorded = deliveries(companyId);
        assertThat(recorded).extracting(d -> d.getSubscription().getId()).containsExactlyInAnyOrder(first, second);
        assertThat(recorded).allSatisfy(delivery -> {
            assertThat(delivery.getStatus()).isEqualTo(WebhookDeliveryStatus.PENDING);
            assertThat(delivery.getAttempts()).isZero();
            assertThat(delivery.getNextAttemptAt()).isNotNull();
            assertThat(delivery.getEventType()).isEqualTo(WebhookEventType.STOCK_AVAILABILITY_CHANGED);
            assertThat(delivery.getEventId()).startsWith("evt_");
            assertThat(delivery.getBody()).contains("\"type\":\"stock.availability_changed\"")
                    .contains(delivery.getEventId());
            assertThat(delivery.getCompany().getId()).isEqualTo(companyId);
        });
        assertThat(recorded).extracting(WebhookDelivery::getEventId).containsOnly(recorded.get(0).getEventId());
        assertThat(recorded).extracting(WebhookDelivery::getBody).containsOnly(recorded.get(0).getBody());
        assertThat(dispatcher.dispatched).containsExactlyInAnyOrderElementsOf(
                recorded.stream().map(WebhookDelivery::getId).toList());
        assertThat(dispatcher.visibleAtDispatch).as("entrega já gravada quando é despachada").containsOnly(true);
        assertThat(afterBusiness).as("despacho só depois do commit").containsExactly(true, true);
    }

    @Test
    void aRolledBackTransactionRecordsAndDispatchesNothing() {
        Long companyId = company();
        subscribe(companyId, true, EnumSet.allOf(WebhookEventType.class));

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            publisher.publishEvent(outOfStock(companyId));
            status.setRollbackOnly();
        });

        assertThat(deliveries(companyId)).isEmpty();
        assertThat(dispatcher.dispatched).isEmpty();
    }

    @Test
    void aFailingDispatchNeitherReachesTheCallerNorUndoesTheOperationNorStopsTheOtherSubscriptions() {
        Long companyId = company();
        subscribe(companyId, true, EnumSet.allOf(WebhookEventType.class));
        subscribe(companyId, true, EnumSet.allOf(WebhookEventType.class));
        dispatcher.onDispatch = id -> {
            throw new IllegalStateException("executor cheio");
        };
        String[] taxId = new String[1];

        inTransaction(em -> {
            taxId[0] = persistCompany(em).getTaxId();
            publisher.publishEvent(outOfStock(companyId));
        });

        assertThat(companyCount(taxId[0])).as("a operação ficou confirmada").isEqualTo(1L);
        assertThat(deliveries(companyId)).hasSize(2);
        assertThat(dispatcher.dispatched).hasSize(2);
    }

    @Test
    void aFailureWhileBuildingTheEventIsCaughtAndRecordsNothing() {
        Long companyId = company();
        subscribe(companyId, true, EnumSet.of(WebhookEventType.ORDER_STATUS_CHANGED));
        when(orderLookup.findOrderForCompany(any(), any())).thenThrow(new IllegalStateException("base indisponível"));
        String[] taxId = new String[1];

        inTransaction(em -> {
            taxId[0] = persistCompany(em).getTaxId();
            publisher.publishEvent(new OrderStatusChangedEvent(companyId, 90L, "ENC-2026/00001", "PENDING",
                    "BILLED", AT));
        });

        assertThat(companyCount(taxId[0])).isEqualTo(1L);
        assertThat(deliveries(companyId)).isEmpty();
        assertThat(dispatcher.dispatched).isEmpty();
    }

    @Test
    void withoutInterestedSubscriptionsNothingIsRecorded() {
        Long companyId = company();
        subscribe(companyId, true, EnumSet.of(WebhookEventType.INVOICE_ISSUED));

        inTransaction(em -> publisher.publishEvent(outOfStock(companyId)));

        assertThat(deliveries(companyId)).isEmpty();
        assertThat(dispatcher.dispatched).isEmpty();
    }

    @Test
    void anEventPublishedOutsideATransactionCountsAsConfirmed() {
        Long companyId = company();
        subscribe(companyId, true, EnumSet.of(WebhookEventType.STOCK_AVAILABILITY_CHANGED));

        publisher.publishEvent(outOfStock(companyId));

        assertThat(deliveries(companyId)).hasSize(1);
        assertThat(dispatcher.dispatched).hasSize(1);
    }

    // ---- helpers ----

    private static StockAvailabilityChangedEvent outOfStock(Long companyId) {
        return new StockAvailabilityChangedEvent(companyId, "ARZ-25", "Arroz 25 kg", "A01", "Loja Central",
                new BigDecimal("3"), BigDecimal.ZERO, AT);
    }

    private Long company() {
        Long[] id = new Long[1];
        inTransaction(em -> id[0] = persistCompany(em).getId());
        return id[0];
    }

    private static Company persistCompany(EntityManager em) {
        int n = SEQ.incrementAndGet();
        Company company = new Company();
        company.setName("Loja Webhook " + n);
        company.setTaxId("webhook-company-" + n);
        em.persist(company);
        return company;
    }

    private Long subscribe(Long companyId, boolean active, EnumSet<WebhookEventType> events) {
        Long[] id = new Long[1];
        inTransaction(em -> {
            WebhookSubscription subscription = new WebhookSubscription();
            subscription.setCompany(em.getReference(Company.class, companyId));
            subscription.setUrl("https://loja.example.com/hooks/" + SEQ.incrementAndGet());
            subscription.setEvents(events);
            subscription.setSecretCiphertext("MCE1:test");
            subscription.setActive(active);
            em.persist(subscription);
            id[0] = subscription.getId();
        });
        return id[0];
    }

    private List<WebhookDelivery> deliveries(Long companyId) {
        return new TransactionTemplate(transactionManager).execute(status -> entityManager.createQuery(
                        "select d from WebhookDelivery d join fetch d.subscription join fetch d.company "
                                + "where d.company.id = :company order by d.id", WebhookDelivery.class)
                .setParameter("company", companyId)
                .getResultList());
    }

    private long companyCount(String taxId) {
        return new TransactionTemplate(transactionManager).execute(status -> entityManager.createQuery(
                        "select count(c) from Company c where c.taxId = :taxId", Long.class)
                .setParameter("taxId", taxId)
                .getSingleResult());
    }

    private void inTransaction(Consumer<EntityManager> work) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> work.accept(entityManager));
    }

    /** Dispatcher that records what it received and whether the delivery was already committed at that moment. */
    static class RecordingDispatcher implements WebhookDispatcher {
        private final EntityManager entityManager;
        private final PlatformTransactionManager transactionManager;
        final List<Long> dispatched = new CopyOnWriteArrayList<>();
        final List<Boolean> visibleAtDispatch = new CopyOnWriteArrayList<>();
        Consumer<Long> onDispatch = id -> { };

        RecordingDispatcher(EntityManager entityManager, PlatformTransactionManager transactionManager) {
            this.entityManager = entityManager;
            this.transactionManager = transactionManager;
        }

        void reset() {
            dispatched.clear();
            visibleAtDispatch.clear();
            onDispatch = id -> { };
        }

        @Override
        public void dispatch(Long deliveryId) {
            dispatched.add(deliveryId);
            TransactionTemplate fresh = new TransactionTemplate(transactionManager);
            fresh.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
            visibleAtDispatch.add(fresh.execute(status ->
                    entityManager.find(WebhookDelivery.class, deliveryId) != null));
            onDispatch.accept(deliveryId);
        }
    }

    @TestConfiguration
    static class Doubles {
        @Bean
        RecordingDispatcher recordingDispatcher(EntityManager entityManager,
                                                PlatformTransactionManager transactionManager) {
            return new RecordingDispatcher(entityManager, transactionManager);
        }

        @Bean
        AuditLogService auditLogService() {
            return mock(AuditLogService.class);
        }

        @Bean
        InvoiceLookup invoiceLookup() {
            return mock(InvoiceLookup.class);
        }

        @Bean
        InvoiceNoteLookup invoiceNoteLookup() {
            return mock(InvoiceNoteLookup.class);
        }

        @Bean
        OrderLookup orderLookup() {
            return mock(OrderLookup.class);
        }
    }
}
