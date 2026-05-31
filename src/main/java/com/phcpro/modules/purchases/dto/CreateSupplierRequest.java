package com.phcpro.modules.purchases.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateSupplierRequest(
        @NotBlank(message = "Nome do fornecedor é obrigatório.") String name,
        @NotBlank(message = "NUIT/NIF é obrigatório.")
        @Pattern(regexp = "\\d{9}", message = "NUIT/NIF deve conter 9 dígitos.") String taxId,
        @Email(message = "Email inválido.") String email,
        String address,
        @NotNull(message = "Empresa é obrigatória.") Long companyId
) {}
