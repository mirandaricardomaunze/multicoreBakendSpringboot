package mz.multicore.erp.modules.webhooks.repository;

import mz.multicore.erp.modules.webhooks.model.WebhookSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, Long> {

    List<WebhookSubscription> findByCompanyIdOrderByCreatedAtDescIdDesc(Long companyId);

    /** Active subscriptions of the company, candidates to receive an event (WH-02). */
    List<WebhookSubscription> findByCompanyIdAndActiveTrue(Long companyId);
}
