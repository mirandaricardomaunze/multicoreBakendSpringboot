package mz.multicore.erp.modules.monitoring.dto;

import java.time.Instant;

/**
 * DTO canónico com o resultado de teste de canais de alarme (e-mail ou som).
 */
public record SystemAlertTestResultDTO(
        String channel, // "EMAIL", "AUDIO"
        boolean success,
        String message,
        Instant timestamp
) {}
