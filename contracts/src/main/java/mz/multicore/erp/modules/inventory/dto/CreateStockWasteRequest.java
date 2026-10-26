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
        String notes,

        @Size(max = 60, message = "A chave de idempotência não pode exceder 60 caracteres.")
        String idempotencyKey
) {
    public CreateStockWasteRequest {
        notes = mz.multicore.erp.architecture.validation.InputSanitizer.sanitizeNotes(notes, 500);
        idempotencyKey = idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim();
    }

    /** Construtor retrocompatível anterior à protecção contra replay. */
    public CreateStockWasteRequest(Long companyId, Long warehouseId, Long productId, Long batchId,
                                   BigDecimal quantity, WasteReason reason, String notes) {
        this(companyId, warehouseId, productId, batchId, quantity, reason, notes, null);
    }
}
