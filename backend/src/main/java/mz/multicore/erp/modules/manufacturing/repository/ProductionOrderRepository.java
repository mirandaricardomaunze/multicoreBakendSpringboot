package mz.multicore.erp.modules.manufacturing.repository;

import mz.multicore.erp.modules.manufacturing.model.ProductionOrderStatus;
import mz.multicore.erp.modules.manufacturing.model.ProductionOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, Long> {
    List<ProductionOrder> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
    List<ProductionOrder> findByCompanyIdAndStatusOrderByCreatedAtDesc(Long companyId, ProductionOrderStatus status);
    Optional<ProductionOrder> findByIdAndCompanyId(Long id, Long companyId);
    boolean existsByCompanyIdAndOrderNumber(Long companyId, String orderNumber);
}
