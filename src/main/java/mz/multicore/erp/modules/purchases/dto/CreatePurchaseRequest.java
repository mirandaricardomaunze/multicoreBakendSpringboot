package mz.multicore.erp.modules.purchases.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreatePurchaseRequest(
        @NotNull(message = "Fornecedor é obrigatório.") Long supplierId,
        @NotNull(message = "Armazém é obrigatório.") Long warehouseId,
        @NotNull(message = "Empresa é obrigatória.") Long companyId,
        // Conta de tesouraria para pagamento imediato. Se null, a compra fica a crédito (conta a pagar).
        Long financeAccountId,
        @NotEmpty(message = "A compra deve conter pelo menos uma linha.") @Valid List<CreatePurchaseLineRequest> lines
) {}
