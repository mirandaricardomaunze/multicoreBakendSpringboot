package mz.multicore.erp.modules.inventory.dto;

import mz.multicore.erp.modules.inventory.model.WasteReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateStockWasteRequest(
        @NotNull(message = "A empresa é obrigatória.")
        Long companyId,

        @NotNull(message = "O armazém é obrigatório.")
        Long warehouseId,

        @NotNull(message = "O produto é obrigatório.")
        Long productId,

        Long batchId,

        @NotNull(message = "A quantidade de quebra é obrigatória.")
        @Positive(message = "A quantidade de quebra deve ser positiva.")
        BigDecimal quantity,

        @NotNull(message = "O motivo da quebra é obrigatório.")
        WasteReason reason,

        @Size(max = 500, message = "As observações não podem exceder 500 caracteres.")
        String notes
) {
    public CreateStockWasteRequest {
        notes = mz.multicore.erp.architecture.validation.InputSanitizer.sanitizeNotes(notes, 500);
    }
}
