package com.phcpro.modules.comercial.repository;

import com.phcpro.modules.comercial.model.CreditNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CreditNoteRepository extends JpaRepository<CreditNote, Long> {

    /**
     * Soma as quantidades já devolvidas para uma {@code InvoiceLine}, ignorando notas
     * rejeitadas ou canceladas. Usado para validar que uma nova NC não excede o que
     * foi vendido nessa linha.
     */
    @Query("SELECT COALESCE(SUM(l.quantity), 0) FROM CreditNoteLine l " +
            "WHERE l.invoiceLine.id = :invoiceLineId " +
            "AND l.creditNote.status <> com.phcpro.modules.comercial.model.NoteStatus.REJECTED " +
            "AND l.creditNote.status <> com.phcpro.modules.comercial.model.NoteStatus.CANCELLED")
    BigDecimal sumNonVoidedReturnedByInvoiceLineId(@Param("invoiceLineId") Long invoiceLineId);

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

    @Query("SELECT n FROM CreditNote n JOIN FETCH n.client JOIN FETCH n.invoice JOIN FETCH n.company " +
            "LEFT JOIN FETCH n.warehouse LEFT JOIN FETCH n.lines l LEFT JOIN FETCH l.product " +
            "WHERE n.id = :id AND n.company.id = :companyId")
    Optional<CreditNote> findByIdWithLinesAndCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);

    long countByCompanyId(Long companyId);
}
