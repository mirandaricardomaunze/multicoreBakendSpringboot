package mz.multicore.erp.modules.hr.repository;

import mz.multicore.erp.modules.hr.model.WorkSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, Long> {
    List<WorkSchedule> findByCompanyIdOrderByName(Long companyId);
    Optional<WorkSchedule> findByIdAndCompanyId(Long id, Long companyId);
    boolean existsByCompanyIdAndNameIgnoreCase(Long companyId, String name);
}
