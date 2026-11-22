package mz.multicore.erp.modules.purchases.repository;

import mz.multicore.erp.modules.purchases.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findByCompanyId(Long companyId);
    List<Supplier> findByCompanyIdAndActiveTrue(Long companyId);
    boolean existsByTaxIdAndCompanyId(String taxId, Long companyId);
    Optional<Supplier> findFirstByTaxIdAndCompanyId(String taxId, Long companyId);
    Optional<Supplier> findByIdAndCompanyId(Long id, Long companyId);
}
