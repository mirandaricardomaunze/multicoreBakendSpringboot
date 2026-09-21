package mz.multicore.erp.modules.inventory.repository;

import mz.multicore.erp.modules.inventory.model.StockWaste;
import mz.multicore.erp.modules.inventory.model.WasteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockWasteRepository extends JpaRepository<StockWaste, Long> {

    @Query("SELECT w FROM StockWaste w " +
            "JOIN FETCH w.warehouse JOIN FETCH w.product " +
            "LEFT JOIN FETCH w.batch " +
            "WHERE w.company.id = :companyId " +
            "ORDER BY w.createdAt DESC")
    List<StockWaste> findByCompanyIdOrderByCreatedAtDesc(@Param("companyId") Long companyId);

    @Query("SELECT w FROM StockWaste w " +
            "JOIN FETCH w.warehouse JOIN FETCH w.product " +
            "LEFT JOIN FETCH w.batch " +
            "WHERE w.company.id = :companyId AND w.status = :status " +
            "ORDER BY w.createdAt DESC")
    List<StockWaste> findByCompanyIdAndStatusOrderByCreatedAtDesc(
            @Param("companyId") Long companyId,
            @Param("status") WasteStatus status);

    @Query("SELECT w FROM StockWaste w " +
            "JOIN FETCH w.warehouse JOIN FETCH w.product " +
            "LEFT JOIN FETCH w.batch " +
            "WHERE w.company.id = :companyId AND w.createdAt >= :startDate AND w.createdAt <= :endDate " +
            "ORDER BY w.createdAt DESC")
    List<StockWaste> findByCompanyIdAndPeriod(
            @Param("companyId") Long companyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
