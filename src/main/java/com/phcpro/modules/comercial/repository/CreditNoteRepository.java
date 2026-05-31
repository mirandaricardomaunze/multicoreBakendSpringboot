package com.phcpro.modules.comercial.repository;

import com.phcpro.modules.comercial.model.CreditNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreditNoteRepository extends JpaRepository<CreditNote, Long> {

    @Query("SELECT n FROM CreditNote n " +
            "JOIN FETCH n.client JOIN FETCH n.invoice " +
            "LEFT JOIN FETCH n.warehouse " +
            "WHERE n.company.id = :companyId " +
            "ORDER BY n.issueDate DESC")
    List<CreditNote> findByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT n FROM CreditNote n " +
            "JOIN FETCH n.client JOIN FETCH n.invoice JOIN FETCH n.company " +
            "LEFT JOIN FETCH n.warehouse " +
            "LEFT JOIN FETCH n.lines l LEFT JOIN FETCH l.product " +
            "WHERE n.id = :id")
    Optional<CreditNote> findByIdWithLines(@Param("id") Long id);

    long countByCompanyId(Long companyId);
}
