package mz.multicore.erp.modules.fleet.dto;

import mz.multicore.erp.modules.fleet.model.FuelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pedido de registo de nova viatura na frota.
 */
public record CreateVehicleRequest(
        @NotBlank(message = "A matrícula da viatura é obrigatória.")
        @Size(max = 20, message = "A matrícula não pode exceder 20 caracteres.")
        String licensePlate,

        @NotBlank(message = "A marca é obrigatória.")
        @Size(max = 50) String make,

        @NotBlank(message = "O modelo é obrigatório.")
        @Size(max = 50) String model,

        int manufacturingYear,

        @NotNull(message = "O tipo de combustível é obrigatório.")
        FuelType fuelType,

        @NotNull(message = "O odómetro inicial é obrigatório.")
        @PositiveOrZero(message = "O odómetro não pode ser negativo.")
        BigDecimal initialOdometerKm,

        String assignedDriverName,
        LocalDate insuranceExpiryDate,
        LocalDate inspectionExpiryDate
) {}
