package mz.multicore.erp.modules.fleet.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registo de abastecimento de combustível para controlo de consumo e custo.
 */
public record FuelLogDTO(
        Long id,
        Long vehicleId,
        String vehicleLicensePlate,
        LocalDateTime logTimestamp,
        BigDecimal odometerKm,
        BigDecimal litersFilled,
        BigDecimal pricePerLiter,
        BigDecimal totalCost,
        String gasStationName,
        String driverName,
        BigDecimal distanceTraveledSinceLastFillKm,
        BigDecimal consumptionRateKmPerLiter
) {}
