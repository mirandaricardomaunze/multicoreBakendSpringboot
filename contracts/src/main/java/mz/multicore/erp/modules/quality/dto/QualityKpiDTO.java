package mz.multicore.erp.modules.quality.dto;

public record QualityKpiDTO(
        long activeAssignmentsCount,
        long expiringSoonCount,
        long expiredCount,
        long expiredWorkersCount,
        long totalEquipmentsCount
) {}
