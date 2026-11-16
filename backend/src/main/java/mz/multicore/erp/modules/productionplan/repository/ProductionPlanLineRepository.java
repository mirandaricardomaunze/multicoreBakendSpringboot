package mz.multicore.erp.modules.productionplan.repository;

import mz.multicore.erp.modules.productionplan.model.ProductionPlanLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductionPlanLineRepository extends JpaRepository<ProductionPlanLine, Long> {

    @Query("select l from ProductionPlanLine l join fetch l.plan p join fetch l.product join fetch l.bom "
            + "where l.id = :id and p.company.id = :companyId")
    Optional<ProductionPlanLine> findByIdAndCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);
}
