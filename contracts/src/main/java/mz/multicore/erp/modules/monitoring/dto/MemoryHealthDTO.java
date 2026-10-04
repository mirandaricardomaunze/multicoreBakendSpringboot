package mz.multicore.erp.modules.monitoring.dto;

public record MemoryHealthDTO(
        long heapUsedBytes,
        long heapMaxBytes,
        double heapUsedPercent,
        long nonHeapUsedBytes
) {}
