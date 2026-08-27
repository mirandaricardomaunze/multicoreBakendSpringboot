package mz.multicore.erp.modules.accounting.dto;

import mz.multicore.erp.modules.accounting.model.JournalSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record JournalEntryDTO(
        Long id,
        String entryNumber,
        LocalDate entryDate,
        String description,
        JournalSource source,
        String sourceLabel,
        String sourceDocumentNumber,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        List<JournalLineDTO> lines
) {}
