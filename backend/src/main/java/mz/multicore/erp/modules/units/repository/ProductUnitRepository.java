package mz.multicore.erp.modules.units.repository;

import mz.multicore.erp.modules.units.model.ProductUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductUnitRepository extends JpaRepository<ProductUnit, Long> {

    List<ProductUnit> findByProductIdOrderByFactorToBase(Long productId);

    Optional<ProductUnit> findByProductIdAndUnitId(Long productId, Long unitId);

    Optional<ProductUnit> findByBarcode(String barcode);

    boolean existsByProductId(Long productId);

    /** Usage checks: once a product appears in stock or documents its base unit can no longer change. */
    @Query("select (count(m) > 0) from StockMovement m where m.product.id = :productId")
    boolean hasStockMovements(@Param("productId") Long productId);

    @Query("select (count(l) > 0) from InvoiceLine l where l.product.id = :productId")
    boolean hasInvoiceLines(@Param("productId") Long productId);

    @Query("select (count(l) > 0) from OrderLine l where l.product.id = :productId")
    boolean hasOrderLines(@Param("productId") Long productId);

    @Query("select (count(l) > 0) from QuotationLine l where l.product.id = :productId")
    boolean hasQuotationLines(@Param("productId") Long productId);

    @Query("select (count(l) > 0) from PurchaseLine l where l.product.id = :productId")
    boolean hasPurchaseLines(@Param("productId") Long productId);
}
