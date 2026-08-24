package mz.multicore.erp.modules.hr.repository;

import mz.multicore.erp.modules.hr.model.SalaryChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SalaryChangeRepository extends JpaRepository<SalaryChange, Long> {

    @Query("SELECT s FROM SalaryChange s JOIN FETCH s.employee "
            + "WHERE s.employee.id = :employeeId AND s.company.id = :companyId "
            + "ORDER BY s.effectiveDate DESC, s.id DESC")
    List<SalaryChange> findByEmployee(@Param("employeeId") Long employeeId,
                                      @Param("companyId") Long companyId);

    /**
     * As alterações já em vigor numa data, da mais recente para a mais antiga. A primeira é a que
     * manda — é o que responde a "quanto ganhava esta pessoa em Março?".
     */
    @Query("SELECT s FROM SalaryChange s WHERE s.employee.id = :employeeId "
            + "AND s.company.id = :companyId AND s.effectiveDate <= :date "
            + "ORDER BY s.effectiveDate DESC, s.id DESC")
    List<SalaryChange> findEffectiveOn(@Param("employeeId") Long employeeId,
                                       @Param("companyId") Long companyId,
                                       @Param("date") LocalDate date);

    boolean existsByCompanyIdAndEmployeeIdAndEffectiveDate(Long companyId, Long employeeId, LocalDate effectiveDate);
}
