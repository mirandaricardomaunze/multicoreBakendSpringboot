package mz.multicore.erp.modules.hr.repository;

import mz.multicore.erp.modules.hr.model.HrPolicyConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HrPolicyConfigRepository extends JpaRepository<HrPolicyConfig, Long> {

    List<HrPolicyConfig> findByCompanyIdOrderByEffectiveFromDesc(Long companyId);

    Optional<HrPolicyConfig> findByIdAndCompanyId(Long id, Long companyId);

    /** As configurações em vigor numa data, da mais recente para a mais antiga. A primeira manda. */
    @Query("SELECT c FROM HrPolicyConfig c WHERE c.company.id = :companyId AND c.active = true "
            + "AND c.effectiveFrom <= :date AND (c.effectiveTo IS NULL OR c.effectiveTo >= :date) "
            + "ORDER BY c.effectiveFrom DESC, c.id DESC")
    List<HrPolicyConfig> findApplicable(@Param("companyId") Long companyId, @Param("date") LocalDate date);
}
