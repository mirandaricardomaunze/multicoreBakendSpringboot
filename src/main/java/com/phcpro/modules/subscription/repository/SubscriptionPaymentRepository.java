package com.phcpro.modules.subscription.repository;

import com.phcpro.modules.subscription.model.SubscriptionPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, Long> {
    List<SubscriptionPayment> findByCompanyIdOrderByPaidAtDesc(Long companyId);

    long countByCompanyId(Long companyId);
}
