package mz.multicore.erp.modules.butchery.repository;

import jakarta.persistence.LockModeType;
import mz.multicore.erp.modules.butchery.model.CarcassBreakdown;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CarcassBreakdownRepository extends JpaRepository<CarcassBreakdown, Long> {

    @Query("select distinct b from CarcassBreakdown b join fetch b.sourceProduct join fetch b.warehouse "
            + "left join fetch b.template left join fetch b.lines l left join fetch l.product "
            + "where b.company.id = :companyId and b.createdAt >= :from order by b.createdAt desc")
    List<CarcassBreakdown> findByCompanySince(@Param("companyId") Long companyId, @Param("from") LocalDateTime from);

    Optional<CarcassBreakdown> findByCompanyIdAndRequestKey(Long companyId, String requestKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from CarcassBreakdown b where b.id = :id and b.company.id = :companyId")
    Optional<CarcassBreakdown> lockByIdAndCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);
}
