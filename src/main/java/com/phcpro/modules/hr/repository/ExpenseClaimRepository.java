package com.phcpro.modules.hr.repository;

import com.phcpro.modules.hr.model.ExpenseClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseClaimRepository extends JpaRepository<ExpenseClaim, Long> {
    List<ExpenseClaim> findByEmployeeId(Long employeeId);

    @Query("SELECT e FROM ExpenseClaim e JOIN FETCH e.employee emp WHERE emp.company.id = :companyId ORDER BY e.createdAt DESC")
    List<ExpenseClaim> findAllWithEmployeeByCompanyId(@Param("companyId") Long companyId);
}
