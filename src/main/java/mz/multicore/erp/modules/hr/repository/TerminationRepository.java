package mz.multicore.erp.modules.hr.repository;

import mz.multicore.erp.modules.hr.model.Termination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TerminationRepository extends JpaRepository<Termination, Long> {

    @Query("SELECT t FROM Termination t JOIN FETCH t.employee WHERE t.company.id = :companyId "
            + "ORDER BY t.terminationDate DESC, t.id DESC")
    List<Termination> findAllByCompany(@Param("companyId") Long companyId);

    @Query("SELECT t FROM Termination t JOIN FETCH t.employee LEFT JOIN FETCH t.lines "
            + "WHERE t.id = :id AND t.company.id = :companyId")
    Optional<Termination> findByIdWithLines(@Param("id") Long id, @Param("companyId") Long companyId);

    Optional<Termination> findByCompanyIdAndEmployeeId(Long companyId, Long employeeId);

    boolean existsByCompanyIdAndEmployeeId(Long companyId, Long employeeId);
}
