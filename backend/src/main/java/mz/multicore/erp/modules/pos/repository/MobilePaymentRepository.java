package mz.multicore.erp.modules.pos.repository;

import mz.multicore.erp.modules.pos.model.MobilePaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MobilePaymentRepository extends JpaRepository<MobilePaymentTransaction, Long> {

    Optional<MobilePaymentTransaction> findByTransactionId(String transactionId);

    Optional<MobilePaymentTransaction> findByTransactionIdAndCompanyId(String transactionId, Long companyId);
}
