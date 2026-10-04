package mz.multicore.erp.modules.monitoring.dto;

public record SubsystemStatusDTO(
        String name,
        String status,
        long latencyMs,
        String details
) {}
