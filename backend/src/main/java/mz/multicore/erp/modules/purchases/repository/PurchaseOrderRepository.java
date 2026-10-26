package mz.multicore.erp.modules.purchases.repository;

import mz.multicore.erp.modules.purchases.model.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findByCompanyIdOrderByOrderDateDesc(Long companyId);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PurchaseOrder o where o.id = :id and o.company.id = :companyId")
    Optional<PurchaseOrder> lockByIdAndCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);
}
