package mz.multicore.erp.modules.comercial.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record OrderPickingConfirmationLineRequest(
    @NotNull @Positive Long lineId,
    @NotNull @Positive BigDecimal confirmedQuantity,
    @Size(max = 120) String batchNumber
) {}
