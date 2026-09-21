package mz.multicore.erp.architecture.events;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Nota de débito aprovada, pronta para reconhecer o acréscimo ao cliente. */
public record DebitNoteApprovedEvent(
        Long companyId,
        Long noteId,
        String noteNumber,
        LocalDate date,
        BigDecimal netAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount
) {}
