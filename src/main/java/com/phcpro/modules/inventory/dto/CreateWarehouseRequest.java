package com.phcpro.modules.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreateWarehouseRequest(
        @NotBlank(message = "Nome do armazem e obrigatorio.") String name,
        @NotBlank(message = "Numero do armazem e obrigatorio.") String warehouseNumber,
        @PositiveOrZero(message = "Capacidade deve ser zero ou superior.") BigDecimal capacity,
        String location,
        @NotNull(message = "Empresa e obrigatoria.") Long companyId
) {}
