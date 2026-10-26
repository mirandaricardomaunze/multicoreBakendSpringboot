package mz.multicore.erp.modules.hr.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mz.multicore.erp.architecture.validation.InputSanitizer;

public record VacationDecisionRequest(
        @NotNull(message = "A decisão de férias é obrigatória.") Boolean approve,
        @Size(max = 500, message = "O motivo de rejeição deve ter no máximo 500 caracteres.")
        String rejectionReason
) {
    public VacationDecisionRequest {
        rejectionReason = InputSanitizer.sanitizeNotes(rejectionReason, 500);
    }
}
