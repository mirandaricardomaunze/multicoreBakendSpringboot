package mz.multicore.erp.modules.fleet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Pedido de criação de Manifesto de Carga a partir de Guias de Remessa ou Encomendas.
 */
public record CreateFleetManifestRequest(
        @NotNull @Positive Long vehicleId,
        @NotBlank @Size(max = 160) String driverName,
        @Size(max = 40) String driverPhone,
        @NotBlank @Size(max = 200) String routeName,
        @Size(max = 40) String departureTime,
        @Size(max = 1000) String notes,
        @NotEmpty @Size(max = 500) List<@NotNull @Positive Long> deliveryGuideIds
) {
    public CreateFleetManifestRequest {
        if (deliveryGuideIds == null) deliveryGuideIds = List.of();
    }
}
