package mz.multicore.erp.modules.hr.repository;

import mz.multicore.erp.modules.hr.model.EmployeeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocument, Long> {

    @Query("SELECT d FROM EmployeeDocument d JOIN FETCH d.employee WHERE d.company.id = :companyId "
            + "ORDER BY CASE WHEN d.expiryDate IS NULL THEN 1 ELSE 0 END, d.expiryDate, d.id")
    List<EmployeeDocument> findAllByCompany(@Param("companyId") Long companyId);

    @Query("SELECT d FROM EmployeeDocument d JOIN FETCH d.employee "
            + "WHERE d.company.id = :companyId AND d.employee.id = :employeeId "
            + "ORDER BY CASE WHEN d.expiryDate IS NULL THEN 1 ELSE 0 END, d.expiryDate, d.id")
    List<EmployeeDocument> findByEmployee(@Param("employeeId") Long employeeId,
                                          @Param("companyId") Long companyId);

    /**
     * Documentos que caducam até uma data — <b>e os que já caducaram</b>, porque um DIRE vencido há
     * duas semanas é mais urgente do que um que vence daqui a cinco dias, e sair da janela não pode
     * ser a forma de o alerta desaparecer.
     */
    @Query("SELECT d FROM EmployeeDocument d JOIN FETCH d.employee "
            + "WHERE d.company.id = :companyId AND d.expiryDate IS NOT NULL AND d.expiryDate <= :limit "
            + "ORDER BY d.expiryDate")
    List<EmployeeDocument> findExpiringUntil(@Param("companyId") Long companyId,
                                             @Param("limit") LocalDate limit);

    Optional<EmployeeDocument> findByIdAndCompanyId(Long id, Long companyId);
}
