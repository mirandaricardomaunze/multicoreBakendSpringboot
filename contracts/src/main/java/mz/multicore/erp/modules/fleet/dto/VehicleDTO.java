package mz.multicore.erp.modules.fleet.dto;

import mz.multicore.erp.modules.fleet.model.FuelType;
import mz.multicore.erp.modules.fleet.model.VehicleStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Representação imutável de uma viatura da frota.
 */
public record VehicleDTO(
        Long id,
        Long companyId,
        String licensePlate,
        String make,
        String model,
        int manufacturingYear,
        FuelType fuelType,
        VehicleStatus status,
        BigDecimal currentOdometerKm,
        String assignedDriverName,
        LocalDate insuranceExpiryDate,
        LocalDate inspectionExpiryDate,
        BigDecimal averageFuelConsumptionKmPerLiter
) {}
