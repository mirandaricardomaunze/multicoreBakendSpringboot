package mz.multicore.erp.modules.performance.repository;

import mz.multicore.erp.modules.performance.model.GoalPeriod;
import mz.multicore.erp.modules.performance.model.GoalScope;
import mz.multicore.erp.modules.performance.model.GoalStatus;
import mz.multicore.erp.modules.performance.model.SalesGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalesGoalRepository extends JpaRepository<SalesGoal, Long> {

    List<SalesGoal> findByCompanyIdOrderByPeriodStartDesc(Long companyId);

    List<SalesGoal> findByCompanyIdAndStatusOrderByPeriodStartDesc(Long companyId, GoalStatus status);

    List<SalesGoal> findByCompanyIdAndStatusAndPeriodOrderByPeriodStartDesc(Long companyId, GoalStatus status, GoalPeriod period);

    List<SalesGoal> findByCompanyIdAndPeriodOrderByPeriodStartDesc(Long companyId, GoalPeriod period);

    List<SalesGoal> findByCompanyIdAndStatus(Long companyId, GoalStatus status);

    List<SalesGoal> findByCompanyIdAndScopeAndScopeRefId(Long companyId, GoalScope scope, Long scopeRefId);

    boolean existsByCompanyIdAndNameAndPeriodStart(Long companyId, String name, LocalDate periodStart);

    Optional<SalesGoal> findByIdAndCompanyId(Long id, Long companyId);
}
