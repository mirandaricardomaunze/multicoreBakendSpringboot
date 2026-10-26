package mz.multicore.erp.modules.inventory.dto;

import jakarta.validation.constraints.Size;
import mz.multicore.erp.architecture.validation.InputSanitizer;

public record RejectStockTransferRequest(
        @Size(max = 500, message = "O motivo de rejeição deve ter no máximo 500 caracteres.")
        String rejectionReason
) {
    public RejectStockTransferRequest {
        rejectionReason = InputSanitizer.sanitizeNotes(rejectionReason, 500);
    }
}
