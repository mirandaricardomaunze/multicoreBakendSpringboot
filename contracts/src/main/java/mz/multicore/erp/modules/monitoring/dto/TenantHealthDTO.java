package mz.multicore.erp.modules.monitoring.dto;

import java.time.Instant;

/**
 * DTO canónico de saúde operacional multi-tenant.
 * Informa o estado de integridade, anomalias, backups e volume de utilizadores de uma empresa.
 */
public record TenantHealthDTO(
        Long companyId,
        String companyName,
        String companyNuit,
        String status, // "HEALTHY", "WARNING", "CRITICAL"
        long activeUsers,
        String lastBackupStatus, // "UP_TO_DATE", "OUTDATED", "FAILED", "NONE"
        int pendingAnomaliesCount,
        Instant lastActivityAt
) {}
