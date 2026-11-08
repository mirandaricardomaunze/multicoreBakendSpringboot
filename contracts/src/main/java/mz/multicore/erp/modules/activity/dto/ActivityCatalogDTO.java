package mz.multicore.erp.modules.activity.dto;

import java.util.List;

/** Full catalogue: available profiles and capabilities (capabilities carry an empty {@code capabilities} list). */
public record ActivityCatalogDTO(
        List<ActivityOptionDTO> profiles,
        List<ActivityOptionDTO> capabilities
) {}
