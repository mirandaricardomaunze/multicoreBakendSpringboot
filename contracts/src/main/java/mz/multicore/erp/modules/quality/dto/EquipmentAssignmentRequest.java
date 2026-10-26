package mz.multicore.erp.modules.quality.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EquipmentAssignmentRequest(
        @NotNull(message = "A empresa é obrigatória.")
        @Positive(message = "A empresa deve ser válida.")
        Long companyId,
        @NotNull(message = "Seleccione o equipamento a atribuir.")
        @Positive(message = "O equipamento deve ser válido.")
        Long equipmentId,
        @NotNull(message = "Seleccione o colaborador que receberá o equipamento.")
        @Positive(message = "O colaborador deve ser válido.")
        Long employeeId,
        LocalDate assignedDate,
        @Positive(message = "A quantidade deve ser superior a zero.")
        int quantity,
        @Size(max = 80, message = "O tamanho ou especificação deve ter no máximo 80 caracteres.")
        String sizeOrSpec,
        @Size(max = 120, message = "O número de série deve ter no máximo 120 caracteres.")
        String serialNumber,
        @Size(max = 1000, message = "As observações devem ter no máximo 1000 caracteres.")
        String notes
) {
    public EquipmentAssignmentRequest(
            Long companyId,
            Long equipmentId,
            Long employeeId,
            LocalDate assignedDate,
            int quantity
    ) {
        this(companyId, equipmentId, employeeId, assignedDate, quantity, null, null, null);
    }
}
