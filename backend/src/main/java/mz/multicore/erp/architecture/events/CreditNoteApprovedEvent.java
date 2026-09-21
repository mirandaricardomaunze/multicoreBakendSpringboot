package mz.multicore.erp.architecture.events;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Nota de crédito aprovada, pronta para estornar a venda no razão. */
public record CreditNoteApprovedEvent(
        Long companyId,
        Long noteId,
        String noteNumber,
        LocalDate date,
        BigDecimal netAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        BigDecimal returnedGoodsCost
) {}
