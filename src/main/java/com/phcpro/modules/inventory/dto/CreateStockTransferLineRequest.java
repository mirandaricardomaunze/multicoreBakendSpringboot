package com.phcpro.modules.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateStockTransferLineRequest(
        @NotNull Long productId,
        @NotNull @Positive BigDecimal quantity
) {}
