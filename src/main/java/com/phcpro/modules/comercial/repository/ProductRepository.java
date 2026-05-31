package com.phcpro.modules.comercial.repository;

import com.phcpro.modules.comercial.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);
    Optional<Product> findByReference(String reference);
    Optional<Product> findByBarcode(String barcode);
}
