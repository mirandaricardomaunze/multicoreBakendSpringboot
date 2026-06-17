package com.phcpro.modules.comercial.repository;

import com.phcpro.modules.comercial.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

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
}
