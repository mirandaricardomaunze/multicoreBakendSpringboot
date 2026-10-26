package mz.multicore.erp.modules.fleet.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Representação imutável de um Manifesto de Carga e Rota de Distribuição.
 */
public record FleetManifestDTO(
        Long id,
        String manifestNumber,
        Long companyId,
        LocalDateTime manifestDate,
        String status,
        Long vehicleId,
        String vehiclePlate,
        String vehicleModel,
        String driverName,
        String driverPhone,
        String routeName,
        String departureTime,
        String notes,
        int totalStops,
        BigDecimal totalBoxes,
        BigDecimal totalPackages,
        BigDecimal totalAmount,
        BigDecimal totalAmountToCollect,
        List<FleetManifestStopDTO> stops
) {
    public FleetManifestDTO {
        if (stops == null) stops = List.of();
        if (totalBoxes == null) totalBoxes = BigDecimal.ZERO.setScale(2);
        if (totalPackages == null) totalPackages = BigDecimal.ZERO.setScale(2);
        if (totalAmount == null) totalAmount = BigDecimal.ZERO;
        if (totalAmountToCollect == null) totalAmountToCollect = BigDecimal.ZERO;
    }
}
