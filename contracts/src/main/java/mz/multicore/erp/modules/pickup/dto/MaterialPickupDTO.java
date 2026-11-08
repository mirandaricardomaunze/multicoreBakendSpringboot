package mz.multicore.erp.modules.pickup.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Pickup slip (LV series): who collected, with which vehicle, and what. */
public record MaterialPickupDTO(
        Long id,
        Long accountId,
        String number,
        LocalDateTime pickedAt,
        String collectorName,
        String collectorDocument,
        String vehiclePlate,
        String notes,
        String status,
        String statusLabel,
        String cancelReason,
        String issuedBy,
        List<MaterialPickupLineDTO> lines
) {}
