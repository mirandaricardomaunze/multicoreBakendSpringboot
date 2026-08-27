package mz.multicore.erp.modules.inventory.repository;

import mz.multicore.erp.modules.inventory.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.product.id = :productId and s.warehouse.id = :warehouseId")
    Optional<Stock> findForReservation(@Param("productId") Long productId, @Param("warehouseId") Long warehouseId);

    @Query("SELECT s FROM Stock s JOIN FETCH s.product JOIN FETCH s.warehouse w JOIN FETCH w.company WHERE w.company.id = :companyId")
    List<Stock> findByWarehouseCompanyId(@Param("companyId") Long companyId);
    
    List<Stock> findByWarehouseId(Long warehouseId);
}
