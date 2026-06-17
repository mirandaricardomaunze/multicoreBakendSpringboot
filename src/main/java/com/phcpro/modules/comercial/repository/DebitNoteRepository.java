package com.phcpro.modules.comercial.repository;

import com.phcpro.modules.comercial.model.DebitNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DebitNoteRepository extends JpaRepository<DebitNote, Long> {

    @Query("SELECT n FROM DebitNote n " +
            "JOIN FETCH n.client JOIN FETCH n.invoice " +
            "WHERE n.company.id = :companyId " +
            "ORDER BY n.issueDate DESC")
    List<DebitNote> findByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT n FROM DebitNote n " +
            "JOIN FETCH n.client JOIN FETCH n.invoice JOIN FETCH n.company " +
            "LEFT JOIN FETCH n.lines " +
            "WHERE n.id = :id")
    Optional<DebitNote> findByIdWithLines(@Param("id") Long id);

    @Query("SELECT n FROM DebitNote n JOIN FETCH n.client JOIN FETCH n.invoice JOIN FETCH n.company " +
            "LEFT JOIN FETCH n.lines WHERE n.id = :id AND n.company.id = :companyId")
    Optional<DebitNote> findByIdWithLinesAndCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);

    long countByCompanyId(Long companyId);
}
