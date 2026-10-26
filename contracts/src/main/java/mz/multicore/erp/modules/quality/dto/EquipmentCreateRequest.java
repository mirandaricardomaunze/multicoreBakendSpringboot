package mz.multicore.erp.modules.quality.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record EquipmentCreateRequest(
        @NotNull(message = "A empresa é obrigatória.")
        @Positive(message = "A empresa deve ser válida.")
        Long companyId,
        @NotBlank(message = "O código do equipamento é obrigatório.")
        @Size(max = 50, message = "O código do equipamento deve ter no máximo 50 caracteres.")
        String code,
        @NotBlank(message = "O nome do equipamento é obrigatório.")
        @Size(max = 160, message = "O nome do equipamento deve ter no máximo 160 caracteres.")
        String name,
        EquipmentCategory category,
        @Positive(message = "O prazo de validade deve ser superior a zero.")
        int periodValue,
        PeriodUnit periodUnit,
        @PositiveOrZero(message = "Os dias de aviso não podem ser negativos.")
        @Max(value = 3650, message = "Os dias de aviso não podem exceder 3650.")
        int warningDaysBefore,
        boolean active,
        @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres.")
        String description
) {
    public EquipmentCreateRequest(
            Long companyId,
            String code,
            String name,
            EquipmentCategory category,
            int periodValue,
            PeriodUnit periodUnit,
            String description
    ) {
        this(companyId, code, name, category, periodValue, periodUnit, 15, true, description);
    }
}
