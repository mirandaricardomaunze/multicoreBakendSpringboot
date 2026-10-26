package mz.multicore.erp.modules.comercial.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Resultado do processamento de uma devolução assistida com Nota de Crédito emitida.
 */
public record SalesReturnWizardResultDTO(
        Long creditNoteId,
        String creditNoteNumber,
        Long invoiceId,
        String invoiceNumber,
        String status,
        BigDecimal totalCredited,
        int restockedLinesCount,
        LocalDateTime createdAt,
        String message
) {
    public SalesReturnWizardResultDTO {
        if (totalCredited == null) totalCredited = BigDecimal.ZERO;
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
