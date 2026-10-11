package mz.multicore.erp.modules.recurring.repository;

import mz.multicore.erp.modules.recurring.model.RecurringBillingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecurringBillingLogRepository extends JpaRepository<RecurringBillingLog, Long> {

    List<RecurringBillingLog> findByContractIdOrderByBillingDateDesc(Long contractId);
}
