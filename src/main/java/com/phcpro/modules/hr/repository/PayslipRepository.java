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

    @Query("SELECT p FROM Payslip p JOIN FETCH p.employee ORDER BY p.year DESC, p.month DESC, p.id DESC")
    List<Payslip> findAllWithEmployee();

    @Query("SELECT p FROM Payslip p JOIN FETCH p.employee WHERE p.id = :id")
    Optional<Payslip> findByIdWithEmployee(@Param("id") Long id);

    Optional<Payslip> findByEmployeeIdAndYearAndMonth(Long employeeId, int year, int month);

    @Query("SELECT p FROM Payslip p JOIN FETCH p.employee WHERE p.year = :year AND p.month = :month")
    List<Payslip> findByYearAndMonth(@Param("year") int year, @Param("month") int month);
}
