package mz.multicore.erp.modules.pos.repository;

import mz.multicore.erp.modules.pos.model.ShiftReconciliation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShiftReconciliationRepository extends JpaRepository<ShiftReconciliation, Long> {
    List<ShiftReconciliation> findByTillSessionIdOrderByReconciledAtAsc(Long tillSessionId);
    List<ShiftReconciliation> findByCompanyIdOrderByReconciledAtDesc(Long companyId);
}
