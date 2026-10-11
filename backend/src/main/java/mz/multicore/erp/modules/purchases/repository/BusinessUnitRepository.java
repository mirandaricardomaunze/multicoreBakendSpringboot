package mz.multicore.erp.modules.purchases.repository;

import mz.multicore.erp.modules.purchases.model.BusinessUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessUnitRepository extends JpaRepository<BusinessUnit, Long> {
    List<BusinessUnit> findByCompanyId(Long companyId);
    List<BusinessUnit> findByCompanyIdAndActiveTrue(Long companyId);
    Optional<BusinessUnit> findByCompanyIdAndCodeIgnoreCase(Long companyId, String code);
    Optional<BusinessUnit> findByIdAndCompanyId(Long id, Long companyId);
    boolean existsByCompanyIdAndCodeIgnoreCase(Long companyId, String code);
}
