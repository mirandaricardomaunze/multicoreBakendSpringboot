package mz.multicore.erp.modules.consignment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record DeliverLineRequest(@NotNull @Positive Long productId, @NotNull @Positive BigDecimal quantity) {}
