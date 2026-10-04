package mz.multicore.erp.modules.monitoring.dto;

public record ThreadPoolHealthDTO(
        int liveThreads,
        int peakThreads,
        long totalStartedThreads,
        int daemonThreads
) {}
