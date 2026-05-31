package com.phcpro.modules.purchases.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreatePurchaseRequest(
        @NotNull(message = "Fornecedor é obrigatório.") Long supplierId,
        @NotNull(message = "Armazém é obrigatório.") Long warehouseId,
        @NotNull(message = "Empresa é obrigatória.") Long companyId,
        @NotNull(message = "Conta financeira é obrigatória.") Long financeAccountId,
        @NotEmpty(message = "A compra deve conter pelo menos uma linha.") @Valid List<CreatePurchaseLineRequest> lines
) {}
