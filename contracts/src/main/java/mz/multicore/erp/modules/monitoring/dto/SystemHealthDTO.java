package mz.multicore.erp.modules.monitoring.dto;

import java.time.Instant;
import java.util.List;

public record SystemHealthDTO(
        String overallStatus,
        long uptimeSeconds,
        String serverVersion,
        String javaVersion,
        String osName,
        DatabaseHealthDTO database,
        StorageHealthDTO storage,
        MemoryHealthDTO memory,
        ThreadPoolHealthDTO threadPool,
        List<SubsystemStatusDTO> subsystems,
        Instant timestamp
) {}
