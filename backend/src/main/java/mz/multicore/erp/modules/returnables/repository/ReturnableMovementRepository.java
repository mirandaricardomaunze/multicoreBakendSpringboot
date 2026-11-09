package mz.multicore.erp.modules.returnables.repository;

import mz.multicore.erp.modules.returnables.model.ReturnableMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnableMovementRepository extends JpaRepository<ReturnableMovement, Long> {

    @Query("select m from ReturnableMovement m join fetch m.client join fetch m.item i join fetch i.product left join fetch m.invoice "
            + "where m.company.id = :companyId order by m.createdAt asc, m.id asc")
    List<ReturnableMovement> findByCompany(@Param("companyId") Long companyId);

    @Query("select m from ReturnableMovement m join fetch m.client join fetch m.item i join fetch i.product left join fetch m.invoice "
            + "where m.company.id = :companyId and m.createdAt >= :from order by m.createdAt desc, m.id desc")
    List<ReturnableMovement> findRecent(@Param("companyId") Long companyId, @Param("from") LocalDateTime from);

    @Query("select m from ReturnableMovement m where m.company.id = :companyId and m.client.id = :clientId and m.item.id = :itemId")
    List<ReturnableMovement> findAccount(@Param("companyId") Long companyId, @Param("clientId") Long clientId, @Param("itemId") Long itemId);

    Optional<ReturnableMovement> findByCompanyIdAndRequestKey(Long companyId, String requestKey);
}
