package com.phcpro.modules.fiscal.repository;

import com.phcpro.modules.fiscal.model.TaxRate;
import com.phcpro.modules.fiscal.model.TaxType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaxRateRepository extends JpaRepository<TaxRate, Long> {
    Optional<TaxRate> findByCode(String code);
    List<TaxRate> findByActiveTrueOrderByTypeAscRateDesc();
    List<TaxRate> findByType(TaxType type);
}
