package com.phcpro.modules.hr.repository;

import com.phcpro.modules.hr.model.PayrollTaxConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PayrollTaxConfigRepository extends JpaRepository<PayrollTaxConfig, Long> {
    boolean existsByCompanyId(Long companyId);

    @Query("""
            SELECT DISTINCT c FROM PayrollTaxConfig c
            LEFT JOIN FETCH c.brackets
            WHERE c.company.id = :companyId AND c.active = true
              AND c.effectiveFrom <= :date
              AND (c.effectiveTo IS NULL OR c.effectiveTo >= :date)
            ORDER BY c.effectiveFrom DESC
            """)
    List<PayrollTaxConfig> findApplicable(@Param("companyId") Long companyId, @Param("date") LocalDate date);

    @Query("SELECT DISTINCT c FROM PayrollTaxConfig c LEFT JOIN FETCH c.brackets WHERE c.company.id = :companyId ORDER BY c.effectiveFrom DESC")
    List<PayrollTaxConfig> findAllByCompanyId(@Param("companyId") Long companyId);
}
