package mz.multicore.erp.modules.inventory.repository;

import mz.multicore.erp.modules.inventory.model.StockAdjustmentOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockAdjustmentOperationRepository extends JpaRepository<StockAdjustmentOperation, Long> {
    Optional<StockAdjustmentOperation> findByCompanyIdAndIdempotencyKey(Long companyId, String idempotencyKey);
}
