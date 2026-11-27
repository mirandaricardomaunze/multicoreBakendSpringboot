package mz.multicore.erp.modules.webhooks.repository;

import mz.multicore.erp.modules.webhooks.model.WebhookDelivery;
import mz.multicore.erp.modules.webhooks.model.WebhookDeliveryStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, Long> {

    /** Due deliveries of active subscriptions, oldest due first (§4.3.2, WH-05). */
    @Query("select d.id from WebhookDelivery d where d.status = :status and d.nextAttemptAt <= :now "
            + "and d.subscription.active = true order by d.nextAttemptAt asc, d.id asc")
    List<Long> findDueIds(@Param("status") WebhookDeliveryStatus status, @Param("now") LocalDateTime now,
                          Pageable page);

    /**
     * Conditional claim of one due attempt (§4.3.2): moves {@code next_attempt_at} to the lease end only when the
     * delivery is still pending and due; 0 rows means another thread or instance already claimed it.
     */
    @Modifying(flushAutomatically = true)
    @Query("update WebhookDelivery d set d.nextAttemptAt = :leaseUntil where d.id = :id and d.status = :status "
            + "and d.nextAttemptAt <= :now")
    int claim(@Param("id") Long id, @Param("status") WebhookDeliveryStatus status, @Param("now") LocalDateTime now,
              @Param("leaseUntil") LocalDateTime leaseUntil);
}
