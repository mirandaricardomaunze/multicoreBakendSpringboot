package com.phcpro.modules.hr.repository;

import com.phcpro.modules.hr.model.Absence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AbsenceRepository extends JpaRepository<Absence, Long> {

    @Query("SELECT a FROM Absence a JOIN FETCH a.employee ORDER BY a.startDate DESC, a.id DESC")
    List<Absence> findAllWithEmployee();
}
