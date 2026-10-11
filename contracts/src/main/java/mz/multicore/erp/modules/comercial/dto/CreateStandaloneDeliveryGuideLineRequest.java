package mz.multicore.erp.modules.comercial.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateStandaloneDeliveryGuideLineRequest(
        @NotNull Long productId,
        @NotNull @Positive BigDecimal quantity
) {}
