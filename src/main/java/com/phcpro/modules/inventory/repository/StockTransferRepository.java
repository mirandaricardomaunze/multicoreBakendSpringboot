package com.phcpro.modules.inventory.repository;

import com.phcpro.modules.inventory.model.StockTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {

    @Query("SELECT t FROM StockTransfer t " +
            "JOIN FETCH t.originWarehouse o " +
            "JOIN FETCH t.destinationWarehouse d " +
            "WHERE t.company.id = :companyId " +
            "ORDER BY t.transferDate DESC")
    List<StockTransfer> findByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT t FROM StockTransfer t " +
            "JOIN FETCH t.originWarehouse " +
            "JOIN FETCH t.destinationWarehouse " +
            "JOIN FETCH t.company " +
            "LEFT JOIN FETCH t.lines l " +
            "LEFT JOIN FETCH l.product " +
            "WHERE t.id = :id")
    Optional<StockTransfer> findByIdWithLines(@Param("id") Long id);

    @Query("SELECT t FROM StockTransfer t JOIN FETCH t.originWarehouse JOIN FETCH t.destinationWarehouse " +
            "JOIN FETCH t.company LEFT JOIN FETCH t.lines l LEFT JOIN FETCH l.product " +
            "WHERE t.id = :id AND t.company.id = :companyId")
    Optional<StockTransfer> findByIdWithLinesAndCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);

    long countByCompanyId(Long companyId);
}
