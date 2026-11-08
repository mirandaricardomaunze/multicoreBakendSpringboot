package mz.multicore.erp.modules.pickup.repository;

import mz.multicore.erp.modules.pickup.model.MaterialPickup;
import mz.multicore.erp.modules.pickup.model.MaterialPickupStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MaterialPickupRepository extends JpaRepository<MaterialPickup, Long> {

    @Query("select distinct p from MaterialPickup p left join fetch p.lines where p.account.id = :accountId order by p.id desc")
    List<MaterialPickup> findByAccountWithLines(@Param("accountId") Long accountId);

    Optional<MaterialPickup> findByIdAndCompanyId(Long id, Long companyId);

    Optional<MaterialPickup> findByCompanyIdAndRequestKey(Long companyId, String requestKey);

    @Query("select max(p.pickedAt) from MaterialPickup p where p.account.id = :accountId and p.status = :status")
    LocalDateTime lastPickupAt(@Param("accountId") Long accountId, @Param("status") MaterialPickupStatus status);

    /** Quantity already handed over per invoice line, counting only issued slips. */
    @Query("select l.invoiceLine.id, sum(l.quantity) from MaterialPickupLine l "
            + "where l.pickup.account.id = :accountId and l.pickup.status = :status group by l.invoiceLine.id")
    List<Object[]> pickedByInvoiceLine(@Param("accountId") Long accountId, @Param("status") MaterialPickupStatus status);
}
