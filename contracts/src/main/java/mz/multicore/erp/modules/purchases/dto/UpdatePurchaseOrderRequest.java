package mz.multicore.erp.modules.purchases.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** Actualização integral de uma encomenda a fornecedor antes de qualquer recepção. */
public record UpdatePurchaseOrderRequest(
        @NotNull(message = "A versão da encomenda é obrigatória.") Long version,
        @NotNull(message = "Fornecedor é obrigatório.") Long supplierId,
        @NotNull(message = "Armazém é obrigatório.") Long warehouseId,
        LocalDate expectedDate,
        @Size(max = 1000, message = "Observações devem ter no máximo 1000 caracteres.")
        String notes,
        @NotEmpty(message = "A encomenda precisa de pelo menos uma linha.") @Valid
        List<CreatePurchaseOrderLineRequest> lines
) {
    public UpdatePurchaseOrderRequest {
        notes = mz.multicore.erp.architecture.validation.InputSanitizer.sanitizeNotes(notes, 1000);
    }
}
