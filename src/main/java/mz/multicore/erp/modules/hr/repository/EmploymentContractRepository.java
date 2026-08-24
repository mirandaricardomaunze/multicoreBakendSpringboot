package mz.multicore.erp.modules.hr.repository;

import mz.multicore.erp.modules.hr.model.ContractStatus;
import mz.multicore.erp.modules.hr.model.EmploymentContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmploymentContractRepository extends JpaRepository<EmploymentContract, Long> {

    @Query("SELECT c FROM EmploymentContract c JOIN FETCH c.employee "
            + "WHERE c.company.id = :companyId ORDER BY c.startDate DESC, c.id DESC")
    List<EmploymentContract> findAllWithEmployeeByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT c FROM EmploymentContract c JOIN FETCH c.employee "
            + "WHERE c.id = :id AND c.company.id = :companyId")
    Optional<EmploymentContract> findByIdWithEmployeeAndCompanyId(@Param("id") Long id,
                                                                  @Param("companyId") Long companyId);

    @Query("SELECT c FROM EmploymentContract c JOIN FETCH c.employee "
            + "WHERE c.employee.id = :employeeId AND c.company.id = :companyId "
            + "ORDER BY c.startDate DESC, c.id DESC")
    List<EmploymentContract> findByEmployeeId(@Param("employeeId") Long employeeId,
                                              @Param("companyId") Long companyId);

    /**
     * Contratos vigentes que se sobrepõem a um intervalo — usada para impedir dois vigentes ao mesmo
     * tempo. Um intervalo aberto (sem fim) sobrepõe-se a tudo o que venha depois do início.
     */
    @Query("SELECT c FROM EmploymentContract c WHERE c.employee.id = :employeeId "
            + "AND c.company.id = :companyId AND c.status = 'VIGENTE' AND c.id <> :excludeId "
            + "AND c.startDate <= :rangeEnd AND (c.endDate IS NULL OR c.endDate >= :rangeStart)")
    List<EmploymentContract> findOverlapping(@Param("employeeId") Long employeeId,
                                             @Param("companyId") Long companyId,
                                             @Param("rangeStart") LocalDate rangeStart,
                                             @Param("rangeEnd") LocalDate rangeEnd,
                                             @Param("excludeId") Long excludeId);

    /** Contratos vigentes com fim à vista — alimenta os alertas de fim de contrato. */
    @Query("SELECT c FROM EmploymentContract c JOIN FETCH c.employee "
            + "WHERE c.company.id = :companyId AND c.status = :status "
            + "AND c.endDate IS NOT NULL AND c.endDate BETWEEN :from AND :to "
            + "ORDER BY c.endDate")
    List<EmploymentContract> findEndingBetween(@Param("companyId") Long companyId,
                                               @Param("status") ContractStatus status,
                                               @Param("from") LocalDate from,
                                               @Param("to") LocalDate to);

    /** Períodos experimentais a terminar — alimenta o segundo aviso, de janela mais curta. */
    @Query("SELECT c FROM EmploymentContract c JOIN FETCH c.employee "
            + "WHERE c.company.id = :companyId AND c.status = :status "
            + "AND c.probationEndDate IS NOT NULL AND c.probationEndDate BETWEEN :from AND :to "
            + "ORDER BY c.probationEndDate")
    List<EmploymentContract> findProbationEndingBetween(@Param("companyId") Long companyId,
                                                        @Param("status") ContractStatus status,
                                                        @Param("from") LocalDate from,
                                                        @Param("to") LocalDate to);

    boolean existsByCompanyIdAndContractNumberIgnoreCase(Long companyId, String contractNumber);
}
