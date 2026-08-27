package mz.multicore.erp.modules.inventory.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Registo de um movimento de stock (ex.: entrada de lote) por ids. */
public record RegisterMovementRequest(
        @NotNull Long productId,
        @NotNull Long warehouseId,
        @NotNull BigDecimal quantity,
        @NotNull String movementType,
        String batchNumber,
        String serialNumber,
        String description,
        LocalDate expirationDate
) {}
