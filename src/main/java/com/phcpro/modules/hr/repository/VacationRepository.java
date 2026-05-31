package com.phcpro.modules.hr.repository;

import com.phcpro.modules.hr.model.Vacation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VacationRepository extends JpaRepository<Vacation, Long> {

    @Query("SELECT v FROM Vacation v JOIN FETCH v.employee ORDER BY v.startDate DESC, v.id DESC")
    List<Vacation> findAllWithEmployee();
}
