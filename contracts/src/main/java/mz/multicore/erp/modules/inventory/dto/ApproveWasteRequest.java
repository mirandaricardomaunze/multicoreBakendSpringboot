package mz.multicore.erp.modules.inventory.dto;

import jakarta.validation.constraints.Size;
import mz.multicore.erp.architecture.validation.InputSanitizer;

public record ApproveWasteRequest(
        boolean approved,
        @Size(max = 500, message = "As observações não podem exceder 500 caracteres.")
        String notes
) {
    public ApproveWasteRequest {
        notes = InputSanitizer.sanitizeNotes(notes, 500);
    }
}
