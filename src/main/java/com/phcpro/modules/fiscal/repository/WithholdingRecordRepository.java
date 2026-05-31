package com.phcpro.modules.fiscal.repository;

import com.phcpro.modules.fiscal.model.WithholdingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WithholdingRecordRepository extends JpaRepository<WithholdingRecord, Long> {

    @Query("SELECT w FROM WithholdingRecord w " +
            "WHERE w.company.id = :companyId " +
            "ORDER BY w.recordDate DESC, w.id DESC")
    List<WithholdingRecord> findByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT w FROM WithholdingRecord w " +
            "WHERE w.company.id = :companyId AND w.recordDate BETWEEN :start AND :end " +
            "ORDER BY w.recordDate ASC")
    List<WithholdingRecord> findByCompanyIdAndPeriod(
            @Param("companyId") Long companyId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);
}
