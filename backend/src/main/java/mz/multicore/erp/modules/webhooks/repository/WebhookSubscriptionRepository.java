package mz.multicore.erp.modules.webhooks.repository;

import mz.multicore.erp.modules.webhooks.model.WebhookSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, Long> {

    List<WebhookSubscription> findByCompanyIdOrderByCreatedAtDescIdDesc(Long companyId);

    /** Active subscriptions of the company, candidates to receive an event (WH-02). */
    List<WebhookSubscription> findByCompanyIdAndActiveTrue(Long companyId);

    /** Subscriptions still off after the automatic deactivation, newest first: the ADMIN alerts (WH-07). */
    List<WebhookSubscription> findByCompanyIdAndActiveFalseAndAutoDisabledAtNotNullOrderByAutoDisabledAtDescIdDesc(
            Long companyId);

    /** Atomic +1 when one delivery of the subscription becomes {@code FAILED} (§4.3.4, WH-07). */
    @Modifying(flushAutomatically = true)
    @Query("update WebhookSubscription s set s.consecutiveFailures = s.consecutiveFailures + 1 where s.id = :id")
    int incrementConsecutiveFailures(@Param("id") Long id);

    /** Back to 0 when one delivery of the subscription is {@code DELIVERED} (§4.3.4, WH-07). */
    @Modifying(flushAutomatically = true)
    @Query("update WebhookSubscription s set s.consecutiveFailures = 0 where s.id = :id "
            + "and s.consecutiveFailures <> 0")
    int resetConsecutiveFailures(@Param("id") Long id);

    /**
     * Conditional automatic deactivation (§4.3.4): only an active subscription that reached the threshold; 1 row
     * means this thread switched it off and must audit, 0 means nothing to do or another thread already did it.
     */
    @Modifying(flushAutomatically = true)
    @Query("update WebhookSubscription s set s.active = false, s.autoDisabledAt = :now where s.id = :id "
            + "and s.active = true and s.consecutiveFailures >= :threshold")
    int autoDisable(@Param("id") Long id, @Param("threshold") int threshold, @Param("now") LocalDateTime now);

    /** Current failure count as stored, read after the bulk updates above. */
    @Query("select s.consecutiveFailures from WebhookSubscription s where s.id = :id")
    int consecutiveFailures(@Param("id") Long id);
}
