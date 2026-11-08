package mz.multicore.erp.modules.consignment.repository;

import jakarta.persistence.LockModeType;
import mz.multicore.erp.modules.consignment.model.ConsignmentDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsignmentDeliveryRepository extends JpaRepository<ConsignmentDelivery, Long> {

    @Query("select distinct d from ConsignmentDelivery d join fetch d.client join fetch d.sourceWarehouse left join fetch d.invoice "
            + "left join fetch d.lines l left join fetch l.product where d.company.id = :companyId "
            + "and (d.status = mz.multicore.erp.modules.consignment.model.ConsignmentStatus.OPEN or d.deliveryDate >= :from) "
            + "order by d.deliveryDate desc, d.id desc")
    List<ConsignmentDelivery> findRecentAndOpen(@Param("companyId") Long companyId, @Param("from") LocalDate from);

    Optional<ConsignmentDelivery> findByCompanyIdAndRequestKey(Long companyId, String requestKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from ConsignmentDelivery d where d.id = :id and d.company.id = :companyId")
    Optional<ConsignmentDelivery> lockByIdAndCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);
}
