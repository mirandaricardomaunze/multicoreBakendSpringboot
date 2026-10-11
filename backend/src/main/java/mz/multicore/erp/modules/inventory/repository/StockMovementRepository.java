package mz.multicore.erp.modules.inventory.repository;

import mz.multicore.erp.modules.inventory.model.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    @Query("SELECT sm FROM StockMovement sm JOIN FETCH sm.product JOIN FETCH sm.warehouse w JOIN FETCH w.company WHERE w.company.id = :companyId ORDER BY sm.movementDate DESC")
    List<StockMovement> findByCompanyId(@Param("companyId") Long companyId);

    @Query(value = "SELECT sm FROM StockMovement sm JOIN FETCH sm.product JOIN FETCH sm.warehouse w "
                    + "JOIN FETCH w.company WHERE w.company.id = :companyId ORDER BY sm.movementDate DESC",
            countQuery = "SELECT count(sm) FROM StockMovement sm JOIN sm.warehouse w "
                    + "WHERE w.company.id = :companyId")
    Page<StockMovement> findPageByCompanyId(@Param("companyId") Long companyId, Pageable pageable);
    
    List<StockMovement> findByProductId(Long productId);
}
