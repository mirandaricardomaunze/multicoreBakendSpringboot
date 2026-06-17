package com.phcpro.modules.hr.repository;

import com.phcpro.modules.hr.model.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayslipRepository extends JpaRepository<Payslip, Long> {

    @Query("SELECT p FROM Payslip p JOIN FETCH p.employee e WHERE e.company.id = :companyId ORDER BY p.year DESC, p.month DESC, p.id DESC")
    List<Payslip> findAllWithEmployeeByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT p FROM Payslip p JOIN FETCH p.employee e WHERE p.id = :id AND e.company.id = :companyId")
    Optional<Payslip> findByIdWithEmployeeAndCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);

    Optional<Payslip> findByEmployeeIdAndYearAndMonth(Long employeeId, int year, int month);

    @Query("SELECT p FROM Payslip p JOIN FETCH p.employee e WHERE e.company.id = :companyId AND p.year = :year AND p.month = :month")
    List<Payslip> findByCompanyIdAndYearAndMonth(@Param("companyId") Long companyId, @Param("year") int year, @Param("month") int month);
}
