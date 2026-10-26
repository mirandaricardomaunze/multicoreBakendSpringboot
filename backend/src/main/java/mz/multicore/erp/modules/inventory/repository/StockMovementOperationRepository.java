package mz.multicore.erp.modules.inventory.repository;

import mz.multicore.erp.modules.inventory.model.StockMovementOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockMovementOperationRepository extends JpaRepository<StockMovementOperation, Long> {
    Optional<StockMovementOperation> findByCompanyIdAndIdempotencyKey(Long companyId, String idempotencyKey);
}
