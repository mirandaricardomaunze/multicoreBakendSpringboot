package mz.multicore.erp.modules.comercial.repository;

import mz.multicore.erp.modules.comercial.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.Collection;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);
    Optional<Product> findByReference(String reference);
    Optional<Product> findByBarcode(String barcode);
    Optional<Product> findBySkuAndCompaniesId(String sku, Long companyId);
    Optional<Product> findByReferenceAndCompaniesId(String reference, Long companyId);
    Optional<Product> findByBarcodeAndCompaniesId(String barcode, Long companyId);
    Optional<Product> findByIdAndCompaniesId(Long id, Long companyId);
    List<Product> findDistinctByCompaniesIdOrderByName(Long companyId);

    @Query("""
            select distinct p from Product p join p.companies c
            where c.id = :companyId
              and (:query = '' or lower(p.name) like lower(concat('%', :query, '%'))
                   or lower(p.sku) like lower(concat('%', :query, '%'))
                   or lower(coalesce(p.reference, '')) like lower(concat('%', :query, '%'))
                   or lower(coalesce(p.barcode, '')) like lower(concat('%', :query, '%')))
              and (:availableOnly = false or p.stockTracked = false or p.id in :sellableIds)
            order by p.name
            """)
    Page<Product> findPOSCatalogPage(
            @Param("companyId") Long companyId,
            @Param("query") String query,
            @Param("availableOnly") boolean availableOnly,
            @Param("sellableIds") Collection<Long> sellableIds,
            Pageable pageable);
}
