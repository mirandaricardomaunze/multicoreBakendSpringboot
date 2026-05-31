package com.phcpro.modules.inventory.repository;

import com.phcpro.modules.inventory.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

    @Query("SELECT s FROM Stock s JOIN FETCH s.product JOIN FETCH s.warehouse w JOIN FETCH w.company WHERE w.company.id = :companyId")
    List<Stock> findByWarehouseCompanyId(@Param("companyId") Long companyId);
    
    List<Stock> findByWarehouseId(Long warehouseId);
}
