package com.phcpro.modules.purchases.dto;

public record SupplierDTO(
        Long id,
        String name,
        String taxId,
        String email,
        String address,
        Long companyId
) {}
