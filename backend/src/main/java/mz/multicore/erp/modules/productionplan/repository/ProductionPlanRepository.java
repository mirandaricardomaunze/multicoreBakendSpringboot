package mz.multicore.erp.modules.productionplan.repository;

import jakarta.persistence.LockModeType;
import mz.multicore.erp.modules.productionplan.model.ProductionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ProductionPlanRepository extends JpaRepository<ProductionPlan, Long> {

    @Query("select p from ProductionPlan p join fetch p.warehouse left join fetch p.lines l left join fetch l.product "
            + "left join fetch l.bom where p.company.id = :companyId and p.warehouse.id = :warehouseId and p.planDate = :date")
    Optional<ProductionPlan> findDay(@Param("companyId") Long companyId, @Param("warehouseId") Long warehouseId,
                                     @Param("date") LocalDate date);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductionPlan p where p.id = :id and p.company.id = :companyId")
    Optional<ProductionPlan> lockByIdAndCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);
}
