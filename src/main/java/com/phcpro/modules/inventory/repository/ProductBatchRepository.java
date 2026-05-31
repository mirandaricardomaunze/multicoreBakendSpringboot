package com.phcpro.modules.inventory.repository;

import com.phcpro.modules.inventory.model.ProductBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductBatchRepository extends JpaRepository<ProductBatch, Long> {

    Optional<ProductBatch> findByProductIdAndWarehouseIdAndBatchNumber(
            Long productId, Long warehouseId, String batchNumber);

    @Query("SELECT b FROM ProductBatch b " +
            "WHERE b.product.id = :productId AND b.warehouse.id = :warehouseId AND b.quantity > 0 " +
            "ORDER BY b.expirationDate ASC, b.entryDate ASC, b.id ASC")
    List<ProductBatch> findAvailableFEFO(@Param("productId") Long productId,
                                          @Param("warehouseId") Long warehouseId);

    @Query("SELECT b FROM ProductBatch b " +
            "JOIN FETCH b.product JOIN FETCH b.warehouse w JOIN FETCH w.company " +
            "WHERE w.company.id = :companyId ORDER BY b.expirationDate ASC")
    List<ProductBatch> findByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT b FROM ProductBatch b " +
            "JOIN FETCH b.product JOIN FETCH b.warehouse " +
            "WHERE b.warehouse.id = :warehouseId ORDER BY b.expirationDate ASC")
    List<ProductBatch> findByWarehouseId(@Param("warehouseId") Long warehouseId);

    @Query("SELECT b FROM ProductBatch b " +
            "JOIN FETCH b.product JOIN FETCH b.warehouse w JOIN FETCH w.company " +
            "WHERE w.company.id = :companyId AND b.expirationDate <= :date AND b.quantity > 0 " +
            "ORDER BY b.expirationDate ASC")
    List<ProductBatch> findExpiringByCompanyId(@Param("companyId") Long companyId,
                                                @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(b.quantity), 0) FROM ProductBatch b " +
            "WHERE b.product.id = :productId AND b.warehouse.id = :warehouseId")
    BigDecimal sumQuantityByProductAndWarehouse(@Param("productId") Long productId,
                                                 @Param("warehouseId") Long warehouseId);
}
