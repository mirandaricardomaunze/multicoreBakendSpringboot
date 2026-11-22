package mz.multicore.erp.modules.purchases.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreatePurchaseOrderRequest(
        @NotNull(message = "Fornecedor é obrigatório.") Long supplierId,
        @NotNull(message = "Armazém é obrigatório.") Long warehouseId,
        @NotNull(message = "Empresa é obrigatória.") Long companyId,
        LocalDate expectedDate,
        @jakarta.validation.constraints.Size(max = 1000, message = "Observações devem ter no máximo 1000 caracteres.")
        String notes,
        @NotEmpty(message = "A encomenda precisa de pelo menos uma linha.")
        @Valid List<CreatePurchaseOrderLineRequest> lines,
        /** Moeda do documento (MZN, USD, ZAR, EUR); vazia = moeda base da empresa. */
        String currency,
        /** Câmbio escrito pelo utilizador; vazio = o proposto pela tabela de câmbios. */
        BigDecimal exchangeRate
) {
    public CreatePurchaseOrderRequest {
        notes = mz.multicore.erp.architecture.validation.InputSanitizer.sanitizeNotes(notes, 1000);
    }

    /** Encomenda na moeda base da empresa (anterior à moeda estrangeira). */
    public CreatePurchaseOrderRequest(Long supplierId, Long warehouseId, Long companyId, LocalDate expectedDate,
                                      String notes, List<CreatePurchaseOrderLineRequest> lines) {
        this(supplierId, warehouseId, companyId, expectedDate, notes, lines, null, null);
    }
}
