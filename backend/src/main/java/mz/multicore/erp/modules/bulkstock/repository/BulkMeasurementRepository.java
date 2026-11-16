package mz.multicore.erp.modules.bulkstock.repository;

import jakarta.persistence.LockModeType;
import mz.multicore.erp.modules.bulkstock.model.BulkMeasurement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BulkMeasurementRepository extends JpaRepository<BulkMeasurement, Long> {

    @Query("select m from BulkMeasurement m join fetch m.product join fetch m.warehouse "
            + "where m.company.id = :companyId order by m.id desc")
    List<BulkMeasurement> findByCompany(@Param("companyId") Long companyId);

    /** Locks the measurement so two managers cannot approve it twice. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from BulkMeasurement m where m.id = :id and m.company.id = :companyId")
    Optional<BulkMeasurement> lockByIdAndCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);
}
