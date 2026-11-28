package mz.multicore.erp.modules.comercial.dto;

import java.time.LocalDate;

/** Estado da entrega de uma guia de remessa e a sua agenda (docs/ENTREGAS_AGENDA_PROVA_SPEC.md §3.1). */
public record DeliveryScheduleDTO(
        Long guideId,
        String guideNumber,
        String deliveryStatus,
        String deliveryStatusLabel,
        LocalDate scheduledDate,
        String scheduledPeriod,
        String scheduledPeriodLabel,
        Long vehicleId,
        String vehicleLabel
) {
}
