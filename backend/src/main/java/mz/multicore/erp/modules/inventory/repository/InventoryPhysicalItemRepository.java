package mz.multicore.erp.modules.inventory.repository;

import mz.multicore.erp.modules.inventory.model.InventoryPhysicalItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryPhysicalItemRepository extends JpaRepository<InventoryPhysicalItem, Long> {

    List<InventoryPhysicalItem> findBySessionId(Long sessionId);

    Optional<InventoryPhysicalItem> findBySessionIdAndProductId(Long sessionId, Long productId);

    Optional<InventoryPhysicalItem> findBySessionIdAndBarcode(Long sessionId, String barcode);
}
