package mz.multicore.erp.modules.fiscal.repository;

import mz.multicore.erp.modules.fiscal.model.TaxRate;
import mz.multicore.erp.modules.fiscal.model.TaxType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaxRateRepository extends JpaRepository<TaxRate, Long> {
    Optional<TaxRate> findByCode(String code);
    List<TaxRate> findByActiveTrueOrderByTypeAscRateDesc();
    List<TaxRate> findByType(TaxType type);
    Optional<TaxRate> findByCodeAndCompaniesId(String code, Long companyId);
    Optional<TaxRate> findByIdAndCompaniesId(Long id, Long companyId);
    List<TaxRate> findDistinctByCompaniesIdOrderByTypeAscRateDesc(Long companyId);
    List<TaxRate> findDistinctByCompaniesIdAndActiveTrueOrderByTypeAscRateDesc(Long companyId);
}
