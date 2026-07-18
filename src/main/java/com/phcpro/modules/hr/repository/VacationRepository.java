package com.phcpro.modules.hr.repository;

import com.phcpro.modules.hr.model.Vacation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VacationRepository extends JpaRepository<Vacation, Long> {

    @Query("SELECT v FROM Vacation v JOIN FETCH v.employee e WHERE e.company.id = :companyId ORDER BY v.startDate DESC, v.id DESC")
    List<Vacation> findAllWithEmployeeByCompanyId(@Param("companyId") Long companyId);

    java.util.Optional<Vacation> findByIdAndEmployeeCompanyId(Long id, Long companyId);

    /** Dias já reservados (aprovados ou pendentes) por colaborador e ano de referência. */
    @Query("SELECT COALESCE(SUM(v.totalDays), 0) FROM Vacation v "
            + "WHERE v.employee.id = :employeeId AND v.yearReference = :year "
            + "AND v.status IN ('APPROVED', 'PENDING')")
    int sumReservedDays(@Param("employeeId") Long employeeId, @Param("year") int year);
}
