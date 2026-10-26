package mz.multicore.erp.modules.comercial.dto;

import jakarta.validation.constraints.Size;
import mz.multicore.erp.architecture.validation.InputSanitizer;

public record RejectionReasonRequest(
        @Size(max = 500, message = "O motivo de rejeição deve ter no máximo 500 caracteres.")
        String rejectionReason
) {
    public RejectionReasonRequest {
        rejectionReason = InputSanitizer.sanitizeNotes(rejectionReason, 500);
    }
}
