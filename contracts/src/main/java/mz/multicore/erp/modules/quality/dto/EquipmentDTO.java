package mz.multicore.erp.modules.quality.dto;

import java.time.LocalDateTime;

public record EquipmentDTO(
        Long id,
        Long companyId,
        String code,
        String name,
        EquipmentCategory category,
        String categoryLabel,
        int periodValue,
        PeriodUnit periodUnit,
        String periodUnitLabel,
        int warningDaysBefore,
        boolean active,
        String description,
        LocalDateTime createdAt
) {
    public EquipmentDTO(
            Long id,
            Long companyId,
            String code,
            String name,
            EquipmentCategory category,
            int periodValue,
            PeriodUnit periodUnit,
            int warningDaysBefore,
            boolean active,
            String description
    ) {
        this(
                id,
                companyId,
                code,
                name,
                category,
                category != null ? category.getLabel() : "-",
                periodValue,
                periodUnit,
                periodUnit != null ? periodUnit.getLabel() : "-",
                warningDaysBefore,
                active,
                description,
                null
        );
    }
}
