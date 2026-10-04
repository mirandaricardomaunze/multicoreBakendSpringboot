package mz.multicore.erp.modules.monitoring.dto;

import java.time.Instant;

/**
 * DTO canónico de incidente e alarme técnico no Multicore ERP.
 */
public record SystemAlertIncidentDTO(
        String incidentId,
        String subsystem,
        String tenantName,
        String severity, // "CRITICAL", "WARNING", "INFO"
        String message,
        Instant timestamp,
        boolean resolved
) {}
