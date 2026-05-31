package com.phcpro.modules.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateStockTransferRequest(
        @NotNull Long companyId,
        @NotNull Long originWarehouseId,
        @NotNull Long destinationWarehouseId,
        String responsible,
        String vehicle,
        String notes,
        @NotEmpty @Valid List<CreateStockTransferLineRequest> lines
) {}
