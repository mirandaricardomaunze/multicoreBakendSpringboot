package com.phcpro.modules.purchases.repository;

import com.phcpro.modules.purchases.model.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    List<Purchase> findByCompanyId(Long companyId);
}
