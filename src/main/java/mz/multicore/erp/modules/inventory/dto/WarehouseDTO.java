package mz.multicore.erp.modules.inventory.dto;

import mz.multicore.erp.modules.inventory.model.WarehouseType;

import java.math.BigDecimal;

public record WarehouseDTO(
        Long id,
        String name,
        String location,
        String warehouseNumber,
        BigDecimal capacity,
        Long companyId,
        WarehouseType type,
        boolean allowsSales,
        String manager,
        String phone,
        boolean active
) {}
