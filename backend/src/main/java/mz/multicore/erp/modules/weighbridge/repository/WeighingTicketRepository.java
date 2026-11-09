package mz.multicore.erp.modules.weighbridge.repository;

import jakarta.persistence.LockModeType;
import mz.multicore.erp.modules.weighbridge.model.WeighingTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeighingTicketRepository extends JpaRepository<WeighingTicket, Long> {

    @Query("select t from WeighingTicket t left join fetch t.product where t.company.id = :companyId "
            + "and (t.status = mz.multicore.erp.modules.weighbridge.model.WeighingStatus.OPEN or t.firstAt >= :from) "
            + "order by t.firstAt desc, t.id desc")
    List<WeighingTicket> findOpenAndRecent(@Param("companyId") Long companyId, @Param("from") LocalDateTime from);

    Optional<WeighingTicket> findByCompanyIdAndRequestKey(Long companyId, String requestKey);

    @Query("select count(t) > 0 from WeighingTicket t where t.company.id = :companyId and upper(t.plate) = upper(:plate) "
            + "and t.status = mz.multicore.erp.modules.weighbridge.model.WeighingStatus.OPEN")
    boolean hasOpenTicket(@Param("companyId") Long companyId, @Param("plate") String plate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from WeighingTicket t where t.id = :id and t.company.id = :companyId")
    Optional<WeighingTicket> lockByIdAndCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);

    @Query("select t from WeighingTicket t left join fetch t.product join fetch t.company where t.id = :id and t.company.id = :companyId")
    Optional<WeighingTicket> findForPrint(@Param("id") Long id, @Param("companyId") Long companyId);
}
