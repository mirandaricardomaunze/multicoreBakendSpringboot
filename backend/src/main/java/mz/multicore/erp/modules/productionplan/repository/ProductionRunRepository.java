package mz.multicore.erp.modules.productionplan.repository;

import mz.multicore.erp.modules.productionplan.model.ProductionRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionRunRepository extends JpaRepository<ProductionRun, Long> {

    @Query("select r from ProductionRun r join fetch r.line l join fetch l.product where l.plan.id = :planId order by r.createdAt desc, r.id desc")
    List<ProductionRun> findByPlan(@Param("planId") Long planId);

    Optional<ProductionRun> findByCompanyIdAndRequestKey(Long companyId, String requestKey);

    @Query("select r.unitCost from ProductionRun r where r.line.product.id = :productId order by r.createdAt desc, r.id desc")
    List<java.math.BigDecimal> lastUnitCosts(@Param("productId") Long productId, org.springframework.data.domain.Pageable page);
}
