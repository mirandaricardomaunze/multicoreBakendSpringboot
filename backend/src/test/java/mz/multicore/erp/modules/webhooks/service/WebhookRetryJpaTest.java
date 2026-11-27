package mz.multicore.erp.modules.webhooks.service;

import mz.multicore.erp.modules.audit.service.AuditLogService;

import jakarta.persistence.EntityManager;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.webhooks.model.WebhookDelivery;
import mz.multicore.erp.modules.webhooks.model.WebhookDeliveryStatus;
import mz.multicore.erp.modules.webhooks.model.WebhookEventType;
import mz.multicore.erp.modules.webhooks.model.WebhookSubscription;
import mz.multicore.erp.modules.webhooks.repository.WebhookDeliveryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Harness WH-05 (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.3.2) against real Hibernate: the due query returns only
 * {@code PENDING} deliveries of active subscriptions whose {@code next_attempt_at} has arrived, oldest first and within
 * the page size; the conditional claim succeeds once and moves {@code next_attempt_at} to the 2-minute lease, so a
 * second claim — another thread or instance — gets nothing until the lease expires.
 * <p>
 * Harness WH-06 (§4.3.3): the delivery-log queries return one subscription's deliveries newest first, optionally of
 * one state, paged; the 30-day cleanup deletes only deliveries created before the cutoff, in batches.
 */
@DataJpaTest(showSql = false, properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "logging.level.org.hibernate.SQL=OFF",
        "logging.level.org.springframework=WARN"
})
@Import({WebhookDeliveryService.class, WebhookRetryJpaTest.FixedClock.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class WebhookRetryJpaTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 28, 9, 0);
    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private WebhookDeliveryRepository repository;
    @Autowired private WebhookDeliveryService deliveries;

    @Test
    void theDueQueryReturnsOnlyDuePendingDeliveriesOfActiveSubscriptionsOldestFirstAndPaged() {
        Long active = subscription(true);
        Long inactive = subscription(false);
        Long dueNow = delivery(active, WebhookDeliveryStatus.PENDING, NOW);
        Long dueEarlier = delivery(active, WebhookDeliveryStatus.PENDING, NOW.minusHours(1));
        delivery(active, WebhookDeliveryStatus.PENDING, NOW.plusSeconds(1));
        delivery(active, WebhookDeliveryStatus.DELIVERED, NOW.minusHours(2));
        delivery(active, WebhookDeliveryStatus.FAILED, null);
        delivery(inactive, WebhookDeliveryStatus.PENDING, NOW.minusHours(3));

        assertThat(deliveries.dueDeliveryIds(50)).containsExactly(dueEarlier, dueNow);
        assertThat(deliveries.dueDeliveryIds(1)).containsExactly(dueEarlier);
    }

    @Test
    void theConditionalClaimSucceedsOnceAndHoldsTheAttemptForTwoMinutes() {
        Long subscription = subscription(true);
        Long id = delivery(subscription, WebhookDeliveryStatus.PENDING, NOW.minusMinutes(1));

        assertThat(deliveries.startAttempt(id)).isPresent();
        assertThat(nextAttemptAt(id)).isEqualTo(NOW.plusMinutes(2));
        assertThat(deliveries.startAttempt(id)).as("já reclamada").isEmpty();
        assertThat(deliveries.dueDeliveryIds(50)).doesNotContain(id);

        assertThat(claim(id, NOW.plusMinutes(2))).as("prazo expirou").isEqualTo(1);
    }

    @Test
    void theClaimIgnoresDeliveriesThatAreNotPendingOrNotDueYet() {
        Long subscription = subscription(true);
        Long delivered = delivery(subscription, WebhookDeliveryStatus.DELIVERED, NOW.minusMinutes(1));
        Long future = delivery(subscription, WebhookDeliveryStatus.PENDING, NOW.plusMinutes(1));

        assertThat(claim(delivered, NOW)).isZero();
        assertThat(claim(future, NOW)).isZero();
        assertThat(nextAttemptAt(future)).isEqualTo(NOW.plusMinutes(1));
    }

    @Test
    void theDeliveryLogIsNewestFirstFilteredByStateAndPaged() {
        Long subscription = subscription(true);
        Long other = subscription(true);
        Long oldest = delivery(subscription, WebhookDeliveryStatus.FAILED, null);
        Long middle = delivery(subscription, WebhookDeliveryStatus.PENDING, NOW.plusDays(1));
        Long newest = delivery(subscription, WebhookDeliveryStatus.PENDING, NOW.plusDays(1));
        delivery(other, WebhookDeliveryStatus.PENDING, NOW.plusDays(1));
        setCreatedAt(oldest, NOW.minusDays(2));
        setCreatedAt(middle, NOW.minusDays(1));
        setCreatedAt(newest, NOW.minusDays(1));

        assertThat(repository.findBySubscriptionIdOrderByCreatedAtDescIdDesc(subscription, PageRequest.of(0, 50)))
                .extracting(WebhookDelivery::getId).containsExactly(newest, middle, oldest);
        assertThat(repository.findBySubscriptionIdOrderByCreatedAtDescIdDesc(subscription, PageRequest.of(1, 2)))
                .extracting(WebhookDelivery::getId).containsExactly(oldest);
        assertThat(repository.findBySubscriptionIdAndStatusOrderByCreatedAtDescIdDesc(subscription,
                WebhookDeliveryStatus.FAILED, PageRequest.of(0, 50)))
                .extracting(WebhookDelivery::getId).containsExactly(oldest);
    }

    @Test
    void theCleanupDeletesOnlyDeliveriesCreatedBeforeTheCutoff() {
        Long subscription = subscription(false);
        Long old = delivery(subscription, WebhookDeliveryStatus.PENDING, NOW);
        Long oldFailed = delivery(subscription, WebhookDeliveryStatus.FAILED, null);
        Long recent = delivery(subscription, WebhookDeliveryStatus.DELIVERED, null);
        setCreatedAt(old, NOW.minusDays(31));
        setCreatedAt(oldFailed, NOW.minusDays(40));
        setCreatedAt(recent, NOW.minusDays(29));

        assertThat(repository.findIdsCreatedBefore(NOW.minusDays(30), PageRequest.of(0, 1))).containsExactly(old);
        while (deliveries.deleteBatchCreatedBefore(NOW.minusDays(30)) > 0) {
            // one batch per transaction until nothing older is left
        }

        assertThat(repository.findById(old)).isEmpty();
        assertThat(repository.findById(oldFailed)).isEmpty();
        assertThat(repository.findById(recent)).isPresent();
        assertThat(entityManager.find(WebhookSubscription.class, subscription)).as("subscrição fica").isNotNull();
    }

    // ---- helpers ----

    /** created_at is set by @PrePersist; tests move it back with a direct update. */
    private void setCreatedAt(Long id, LocalDateTime createdAt) {
        inTransaction(em -> em.createQuery("update WebhookDelivery d set d.createdAt = :at where d.id = :id")
                .setParameter("at", createdAt).setParameter("id", id).executeUpdate());
    }

    private int claim(Long id, LocalDateTime now) {
        return new TransactionTemplate(transactionManager).execute(status ->
                repository.claim(id, WebhookDeliveryStatus.PENDING, now, now.plusMinutes(2)));
    }

    private LocalDateTime nextAttemptAt(Long id) {
        return new TransactionTemplate(transactionManager).execute(status ->
                entityManager.find(WebhookDelivery.class, id).getNextAttemptAt());
    }

    private Long subscription(boolean active) {
        Long[] id = new Long[1];
        inTransaction(em -> {
            int n = SEQ.incrementAndGet();
            Company company = new Company();
            company.setName("Loja Retry " + n);
            company.setTaxId("webhook-retry-" + n);
            em.persist(company);
            WebhookSubscription subscription = new WebhookSubscription();
            subscription.setCompany(company);
            subscription.setUrl("https://loja.example.com/hooks/" + n);
            subscription.setEvents(EnumSet.of(WebhookEventType.INVOICE_ISSUED));
            subscription.setSecretCiphertext("MCE1:test");
            subscription.setActive(active);
            em.persist(subscription);
            id[0] = subscription.getId();
        });
        return id[0];
    }

    private Long delivery(Long subscriptionId, WebhookDeliveryStatus status, LocalDateTime nextAttemptAt) {
        Long[] id = new Long[1];
        inTransaction(em -> {
            WebhookSubscription subscription = em.find(WebhookSubscription.class, subscriptionId);
            WebhookDelivery delivery = new WebhookDelivery();
            delivery.setSubscription(subscription);
            delivery.setCompany(subscription.getCompany());
            delivery.setEventId("evt_" + SEQ.incrementAndGet());
            delivery.setEventType(WebhookEventType.INVOICE_ISSUED);
            delivery.setBody("{}");
            delivery.setStatus(status);
            delivery.setNextAttemptAt(nextAttemptAt);
            em.persist(delivery);
            id[0] = delivery.getId();
        });
        return id[0];
    }

    private void inTransaction(Consumer<EntityManager> work) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> work.accept(entityManager));
    }

    @TestConfiguration
    static class FixedClock {
        @Bean
        AuditLogService auditLogService() {
            return mock(AuditLogService.class);
        }

        @Bean
        WebhookRetryPolicy webhookRetryPolicy() {
            return new WebhookRetryPolicy(Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
        }
    }
}
