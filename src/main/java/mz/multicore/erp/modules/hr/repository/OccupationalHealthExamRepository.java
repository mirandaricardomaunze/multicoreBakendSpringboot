package mz.multicore.erp.modules.hr.repository;

import mz.multicore.erp.modules.hr.model.OccupationalHealthExam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OccupationalHealthExamRepository extends JpaRepository<OccupationalHealthExam, Long> {
    @Query("select e from OccupationalHealthExam e join fetch e.employee "
            + "where e.company.id=:companyId and e.employee.id=:employeeId "
            + "order by e.examDate desc, e.id desc")
    List<OccupationalHealthExam> findHistory(@Param("companyId") Long companyId,
                                             @Param("employeeId") Long employeeId);

    Optional<OccupationalHealthExam> findFirstByCompanyIdAndEmployeeIdOrderByExamDateDescIdDesc(
            Long companyId, Long employeeId);

    @Query("select e from OccupationalHealthExam e join fetch e.employee "
            + "where e.company.id=:companyId and e.expiryDate<=:limit "
            + "and e.examDate=(select max(x.examDate) from OccupationalHealthExam x "
            + "where x.company.id=:companyId and x.employee.id=e.employee.id) order by e.expiryDate")
    List<OccupationalHealthExam> findLatestExpiring(@Param("companyId") Long companyId,
                                                    @Param("limit") LocalDate limit);
}
