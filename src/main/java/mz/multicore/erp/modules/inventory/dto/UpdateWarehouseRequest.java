package mz.multicore.erp.modules.inventory.dto;

import mz.multicore.erp.modules.inventory.model.WarehouseType;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/** Edição de um armazém (PUT /warehouses/{id}). */
public record UpdateWarehouseRequest(
        @NotBlank String name,
        String warehouseNumber,
        BigDecimal capacity,
        String location,
        WarehouseType type,
        boolean allowsSales,
        String manager,
        String phone
) {}
