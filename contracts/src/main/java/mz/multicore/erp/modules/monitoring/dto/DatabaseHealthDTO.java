package mz.multicore.erp.modules.monitoring.dto;

public record DatabaseHealthDTO(
        String status,
        long responseTimeMs,
        int activeConnections,
        int idleConnections,
        int maxConnections,
        String databaseEngine
) {}
