package com.phcpro.modules.comercial.repository;

import com.phcpro.modules.comercial.model.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
    Optional<ProductCategory> findByCode(String code);
    List<ProductCategory> findByActiveTrueOrderByNameAsc();
    List<ProductCategory> findAllByOrderByNameAsc();
    Optional<ProductCategory> findByCodeAndCompaniesId(String code, Long companyId);
    Optional<ProductCategory> findByIdAndCompaniesId(Long id, Long companyId);
    List<ProductCategory> findDistinctByCompaniesIdOrderByNameAsc(Long companyId);
    List<ProductCategory> findDistinctByCompaniesIdAndActiveTrueOrderByNameAsc(Long companyId);
}
