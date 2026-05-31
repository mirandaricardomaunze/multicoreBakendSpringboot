package com.phcpro.modules.inventory.dto;

import java.math.BigDecimal;

public record WarehouseDTO(
        Long id,
        String name,
        String location,
        String warehouseNumber,
        BigDecimal capacity,
        Long companyId
) {}
