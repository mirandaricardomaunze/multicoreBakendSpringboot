package mz.multicore.erp.modules.manufacturing.repository;

import mz.multicore.erp.modules.manufacturing.model.BillOfMaterials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillOfMaterialsRepository extends JpaRepository<BillOfMaterials, Long> {
    List<BillOfMaterials> findByCompanyIdAndActiveTrueOrderByNameAsc(Long companyId);
    List<BillOfMaterials> findByCompanyIdOrderByNameAsc(Long companyId);
    Optional<BillOfMaterials> findByIdAndCompanyId(Long id, Long companyId);
    boolean existsByCompanyIdAndBomCode(Long companyId, String bomCode);
}
