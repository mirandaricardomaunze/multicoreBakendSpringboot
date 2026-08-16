package com.phcpro.modules.accounting.repository;

import com.phcpro.modules.accounting.model.JournalEntry;
import com.phcpro.modules.accounting.model.JournalSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    /** Lançamentos de um período — a consulta base do razão e do balancete. */
    List<JournalEntry> findByCompanyIdAndEntryDateBetweenOrderByEntryDateAscIdAsc(
            Long companyId, LocalDate from, LocalDate to);

    Page<JournalEntry> findByCompanyIdOrderByEntryDateDescIdDesc(Long companyId, Pageable pageable);

    /**
     * Lançamento já gerado para um documento — impede que o mesmo documento seja lançado duas
     * vezes (uma repetição de pedido HTTP duplicaria as vendas na contabilidade).
     */
    Optional<JournalEntry> findByCompanyIdAndSourceAndSourceDocumentId(
            Long companyId, JournalSource source, Long sourceDocumentId);
}
