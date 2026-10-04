package mz.multicore.erp.modules.comercial.dto;

import jakarta.validation.constraints.Size;

/**
 * Motivo de anulação/cancelamento (fatura, encomenda, recibo).
 *
 * <p>A obrigatoriedade do motivo é regra de negócio e vive nos serviços (a anulação de cotação,
 * por exemplo, aceita corpo opcional); aqui só se limita o tamanho.
 */
public record CancelReasonRequest(
        @Size(max = 500, message = "O motivo não pode exceder 500 caracteres.")
        String reason
) {
    public CancelReasonRequest {
        reason = mz.multicore.erp.architecture.validation.InputSanitizer.sanitizeNotes(reason, 500);
    }
}
