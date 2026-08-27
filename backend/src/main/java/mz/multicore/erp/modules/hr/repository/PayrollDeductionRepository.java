package mz.multicore.erp.modules.hr.repository;

import mz.multicore.erp.modules.hr.model.PayrollDeduction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollDeductionRepository extends JpaRepository<PayrollDeduction, Long> {

    @Query("SELECT d FROM PayrollDeduction d JOIN FETCH d.employee "
            + "WHERE d.company.id = :companyId ORDER BY d.active DESC, d.startDate DESC, d.id DESC")
    List<PayrollDeduction> findAllByCompany(@Param("companyId") Long companyId);

    @Query("SELECT d FROM PayrollDeduction d JOIN FETCH d.employee "
            + "WHERE d.company.id = :companyId AND d.employee.id = :employeeId "
            + "ORDER BY d.active DESC, d.startDate DESC, d.id DESC")
    List<PayrollDeduction> findByEmployee(@Param("employeeId") Long employeeId,
                                          @Param("companyId") Long companyId);

    /**
     * Os compromissos activos de um colaborador que cobrem um período. A ordem é deliberada: quando
     * o salário não chega para tudo, desconta-se primeiro o mais antigo — senão a ordem dependia da
     * base de dados e o colaborador não conseguia prever o seu próprio líquido.
     */
    @Query("SELECT d FROM PayrollDeduction d JOIN FETCH d.employee "
            + "WHERE d.company.id = :companyId AND d.employee.id = :employeeId AND d.active = true "
            + "AND d.startDate <= :periodEnd AND (d.endDate IS NULL OR d.endDate >= :periodEnd) "
            + "ORDER BY d.startDate, d.id")
    List<PayrollDeduction> findApplicable(@Param("employeeId") Long employeeId,
                                          @Param("companyId") Long companyId,
                                          @Param("periodEnd") LocalDate periodEnd);

    Optional<PayrollDeduction> findByIdAndCompanyId(Long id, Long companyId);
}
