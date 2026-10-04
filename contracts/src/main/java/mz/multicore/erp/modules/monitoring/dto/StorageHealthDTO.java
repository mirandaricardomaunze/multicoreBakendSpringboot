package mz.multicore.erp.modules.monitoring.dto;

public record StorageHealthDTO(
        long totalBytes,
        long freeBytes,
        long usedBytes,
        double usedPercent,
        String storagePath
) {}
