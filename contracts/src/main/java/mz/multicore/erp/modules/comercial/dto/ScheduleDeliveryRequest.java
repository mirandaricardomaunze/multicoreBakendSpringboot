package mz.multicore.erp.modules.comercial.dto;

import java.time.LocalDate;

/**
 * Agendar ou reagendar a entrega de uma guia de remessa aprovada: dia, período ({@code MORNING} /
 * {@code AFTERNOON}) e viatura da frota. As regras vivem no serviço (docs/ENTREGAS_AGENDA_PROVA_SPEC.md §3.1).
 */
public record ScheduleDeliveryRequest(
        LocalDate scheduledDate,
        String scheduledPeriod,
        Long vehicleId
) {
}
