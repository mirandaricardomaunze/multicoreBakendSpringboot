package mz.multicore.erp.modules.purchases.repository;

import mz.multicore.erp.modules.purchases.model.SupplierReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierReturnRepository extends JpaRepository<SupplierReturn, Long> {
    List<SupplierReturn> findByCompanyIdOrderByReturnDateDesc(Long companyId);
}
