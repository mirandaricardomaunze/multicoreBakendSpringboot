package mz.multicore.erp.modules.purchases.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreatePurchaseOrderRequest(
        @NotNull(message = "Fornecedor é obrigatório.") Long supplierId,
        @NotNull(message = "Armazém é obrigatório.") Long warehouseId,
        @NotNull(message = "Empresa é obrigatória.") Long companyId,
        LocalDate expectedDate,
        String notes,
        @NotEmpty(message = "A encomenda precisa de pelo menos uma linha.")
        @Valid List<CreatePurchaseOrderLineRequest> lines
) {}
