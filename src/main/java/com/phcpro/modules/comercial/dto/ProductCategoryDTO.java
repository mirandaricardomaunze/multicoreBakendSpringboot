package com.phcpro.modules.comercial.dto;

public record ProductCategoryDTO(
        Long id,
        String code,
        String name,
        String colorHex,
        boolean active
) {}
