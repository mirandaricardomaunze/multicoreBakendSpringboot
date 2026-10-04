package mz.multicore.erp.modules.inventory.dto;

import mz.multicore.erp.architecture.validation.ValidPhoneMZ;
import mz.multicore.erp.modules.inventory.model.WarehouseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateWarehouseRequest(
        @NotBlank(message = "Nome do armazém é obrigatório.")
        @Size(max = 120, message = "O nome do armazém não pode exceder 120 caracteres.")
        String name,

        @NotBlank(message = "Número do armazém é obrigatório.")
        @Size(max = 30, message = "O código do armazém não pode exceder 30 caracteres.")
        String warehouseNumber,

        @PositiveOrZero(message = "Capacidade deve ser zero ou superior.")
        BigDecimal capacity,

        @Size(max = 255, message = "A localização não pode exceder 255 caracteres.")
        String location,

        @NotNull(message = "Empresa é obrigatória.")
        Long companyId,

        WarehouseType type,
        boolean allowsSales,

        @Size(max = 100, message = "O nome do responsável não pode exceder 100 caracteres.")
        String manager,

        @ValidPhoneMZ
        String phone
) {}
