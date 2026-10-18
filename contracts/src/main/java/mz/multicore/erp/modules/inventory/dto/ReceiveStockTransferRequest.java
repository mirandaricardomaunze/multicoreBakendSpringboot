package mz.multicore.erp.modules.inventory.dto;

import jakarta.validation.constraints.Size;

/** Confirmação de recepção de uma guia em trânsito pelo armazém de destino. */
public record ReceiveStockTransferRequest(
        @Size(max = 500, message = "As observações da recepção não podem exceder 500 caracteres.")
        String notes
) {
    public ReceiveStockTransferRequest {
        notes = mz.multicore.erp.architecture.validation.InputSanitizer.sanitizeNotes(notes, 500);
    }
}
