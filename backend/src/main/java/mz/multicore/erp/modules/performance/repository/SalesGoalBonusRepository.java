package mz.multicore.erp.modules.performance.repository;

import mz.multicore.erp.modules.performance.model.BonusStatus;
import mz.multicore.erp.modules.performance.model.SalesGoalBonus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalesGoalBonusRepository extends JpaRepository<SalesGoalBonus, Long> {

    List<SalesGoalBonus> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    List<SalesGoalBonus> findByCompanyIdAndStatusOrderByCreatedAtDesc(Long companyId, BonusStatus status);

    List<SalesGoalBonus> findByCompanyIdAndEmployeeIdAndStatus(Long companyId, Long employeeId, BonusStatus status);

    List<SalesGoalBonus> findByGoalId(Long goalId);

    Optional<SalesGoalBonus> findByIdAndCompanyId(Long id, Long companyId);

    Optional<SalesGoalBonus> findByGoalIdAndEmployeeId(Long goalId, Long employeeId);
}
