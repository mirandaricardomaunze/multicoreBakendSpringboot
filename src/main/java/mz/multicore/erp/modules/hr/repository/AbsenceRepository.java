package mz.multicore.erp.modules.hr.repository;

import mz.multicore.erp.modules.hr.model.Absence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AbsenceRepository extends JpaRepository<Absence, Long> {

    @Query("SELECT a FROM Absence a JOIN FETCH a.employee e WHERE e.company.id = :companyId ORDER BY a.startDate DESC, a.id DESC")
    List<Absence> findAllWithEmployeeByCompanyId(@Param("companyId") Long companyId);

    /**
     * Faltas <b>não remuneradas</b> de um colaborador que se sobrepõem a um período.
     *
     * <p>Os tipos vêm do {@code AbsencePayRule} em vez de estarem escritos aqui: a regra de quais as
     * faltas que descontam é uma decisão de negócio, e tê-la enterrada num literal desta consulta
     * fazia com que qualquer tipo novo passasse a ser pago sem ninguém ter decidido isso (§B8.5).
     */
    @Query("SELECT a FROM Absence a WHERE a.employee.id = :employeeId AND a.absenceType IN :unpaidTypes "
            + "AND a.startDate <= :periodEnd AND a.endDate >= :periodStart")
    List<Absence> findUnpaidOverlapping(@Param("employeeId") Long employeeId,
                                        @Param("periodStart") LocalDate periodStart,
                                        @Param("periodEnd") LocalDate periodEnd,
                                        @Param("unpaidTypes") java.util.Collection<String> unpaidTypes);

    /** Falta da empresa activa, com o colaborador carregado — a auditoria da eliminação precisa do nome. */
    @Query("SELECT a FROM Absence a JOIN FETCH a.employee e WHERE a.id = :id AND e.company.id = :companyId")
    Optional<Absence> findByIdAndEmployeeCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);

    /** Já existe falta nesse dia — torna a geração automática pelo fecho do ponto idempotente. */
    boolean existsByEmployeeIdAndStartDate(Long employeeId, LocalDate startDate);

    boolean existsByIdAndEmployeeCompanyId(Long id, Long companyId);

    void deleteByIdAndEmployeeCompanyId(Long id, Long companyId);
}
