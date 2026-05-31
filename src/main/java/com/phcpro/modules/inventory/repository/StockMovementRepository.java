package com.phcpro.modules.inventory.repository;

import com.phcpro.modules.inventory.model.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    @Query("SELECT sm FROM StockMovement sm JOIN FETCH sm.product JOIN FETCH sm.warehouse w JOIN FETCH w.company WHERE w.company.id = :companyId ORDER BY sm.movementDate DESC")
    List<StockMovement> findByCompanyId(@Param("companyId") Long companyId);
    
    List<StockMovement> findByProductId(Long productId);
}
