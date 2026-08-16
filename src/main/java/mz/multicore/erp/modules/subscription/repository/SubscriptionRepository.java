package mz.multicore.erp.modules.subscription.repository;

import mz.multicore.erp.modules.subscription.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByCompanyId(Long companyId);
}
