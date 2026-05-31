package com.phcpro.modules.hr.repository;

import com.phcpro.modules.hr.model.ExpenseClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseClaimRepository extends JpaRepository<ExpenseClaim, Long> {
    List<ExpenseClaim> findByEmployeeId(Long employeeId);
}
