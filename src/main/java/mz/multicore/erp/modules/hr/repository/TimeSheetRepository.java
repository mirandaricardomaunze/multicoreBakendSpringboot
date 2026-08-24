package mz.multicore.erp.modules.hr.repository;

import mz.multicore.erp.modules.hr.model.TimeSheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TimeSheetRepository extends JpaRepository<TimeSheet, Long> {
    Optional<TimeSheet> findByCompanyIdAndYearAndMonth(Long companyId, int year, int month);
}
