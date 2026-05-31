package com.phcpro.modules.comercial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProductCategoryRequest(
        @NotBlank @Size(max = 30) String code,
        @NotBlank String name,
        @Size(max = 9) String colorHex
) {}
