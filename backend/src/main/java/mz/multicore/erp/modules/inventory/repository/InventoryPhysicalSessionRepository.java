package mz.multicore.erp.modules.inventory.repository;

import mz.multicore.erp.modules.inventory.model.InventoryPhysicalSession;
import mz.multicore.erp.modules.inventory.dto.InventoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryPhysicalSessionRepository extends JpaRepository<InventoryPhysicalSession, Long> {

    List<InventoryPhysicalSession> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    List<InventoryPhysicalSession> findByCompanyIdAndStatus(Long companyId, InventoryStatus status);

    Optional<InventoryPhysicalSession> findByIdAndCompanyId(Long id, Long companyId);

    boolean existsByCompanyIdAndStatus(Long companyId, InventoryStatus status);
}
