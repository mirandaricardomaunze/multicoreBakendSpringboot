package mz.multicore.erp.modules.accounting.repository;

import mz.multicore.erp.modules.accounting.model.CostCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CostCenterRepository extends JpaRepository<CostCenter, Long> {

    List<CostCenter> findByCompanyIdOrderByCode(Long companyId);

    boolean existsByCompanyIdAndCodeIgnoreCase(Long companyId, String code);

    /** Há linhas de lançamento marcadas com este centro (impede apagar). */
    @Query("select count(l) > 0 from JournalLine l where l.costCenter.id = :costCenterId")
    boolean hasJournalLines(@Param("costCenterId") Long costCenterId);
}
