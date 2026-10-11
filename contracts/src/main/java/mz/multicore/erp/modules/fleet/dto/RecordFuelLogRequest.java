package mz.multicore.erp.modules.fleet.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pedido de registo de abastecimento de combustível.
 */
public record RecordFuelLogRequest(
        @NotNull(message = "A viatura é obrigatória.")
        Long vehicleId,

        LocalDateTime logTimestamp,

        @NotNull(message = "O odómetro no momento do abastecimento é obrigatório.")
        @Positive(message = "O odómetro deve ser positivo.")
        BigDecimal odometerKm,

        @NotNull(message = "A quantidade de litros é obrigatória.")
        @Positive(message = "A quantidade deve ser positiva.")
        BigDecimal litersFilled,

        @NotNull(message = "O preço por litro é obrigatório.")
        @Positive(message = "O preço deve ser positivo.")
        BigDecimal pricePerLiter,

        String gasStationName,
        String driverName
) {}
