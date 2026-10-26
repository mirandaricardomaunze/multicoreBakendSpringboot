package mz.multicore.erp.modules.inventory.repository;

import mz.multicore.erp.modules.inventory.model.InventoryCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;

public interface InventoryCountRepository extends JpaRepository<InventoryCount, Long> {

    List<InventoryCount> findByCompany_IdOrderByCreatedAtDesc(Long companyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select distinct c from InventoryCount c "
            + "left join fetch c.lines l left join fetch l.product "
            + "join fetch c.company join fetch c.warehouse where c.id = :id")
    java.util.Optional<InventoryCount> findByIdForUpdate(@Param("id") Long id);
}
