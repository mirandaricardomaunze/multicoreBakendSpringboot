package mz.multicore.erp.modules.activity.dto;

import java.util.List;

/** Option shown when choosing profiles: code, Mozambican Portuguese text and enabled capabilities. */
public record ActivityOptionDTO(
        String code,
        String label,
        String description,
        List<String> capabilities
) {}
