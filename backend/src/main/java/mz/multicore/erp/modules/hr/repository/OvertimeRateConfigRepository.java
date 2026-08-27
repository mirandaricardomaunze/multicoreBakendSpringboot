package mz.multicore.erp.modules.hr.repository;

import mz.multicore.erp.modules.hr.model.OvertimeRateConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OvertimeRateConfigRepository extends JpaRepository<OvertimeRateConfig, Long> {
    List<OvertimeRateConfig> findByCompanyIdOrderByEffectiveFromDesc(Long companyId);
    Optional<OvertimeRateConfig> findByIdAndCompanyId(Long id, Long companyId);
}
