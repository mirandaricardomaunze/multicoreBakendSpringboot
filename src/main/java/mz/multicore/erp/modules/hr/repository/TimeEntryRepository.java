package mz.multicore.erp.modules.hr.repository;

import mz.multicore.erp.modules.hr.model.TimeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {

    @Query("SELECT t FROM TimeEntry t JOIN FETCH t.employee "
            + "WHERE t.company.id = :companyId AND t.entryDate BETWEEN :from AND :to "
            + "ORDER BY t.entryDate, t.employee.name")
    List<TimeEntry> findByPeriod(@Param("companyId") Long companyId,
                                 @Param("from") LocalDate from,
                                 @Param("to") LocalDate to);

    @Query("SELECT t FROM TimeEntry t JOIN FETCH t.employee "
            + "WHERE t.id = :id AND t.company.id = :companyId")
    Optional<TimeEntry> findByIdAndCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);

    /** Anti-duplicação: uma marcação por colaborador e dia (espelha o índice único da V50). */
    boolean existsByCompanyIdAndEmployeeIdAndEntryDate(Long companyId, Long employeeId, LocalDate entryDate);
}
