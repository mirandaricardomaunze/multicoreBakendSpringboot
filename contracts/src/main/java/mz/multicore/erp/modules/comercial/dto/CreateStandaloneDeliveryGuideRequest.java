package mz.multicore.erp.modules.comercial.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mz.multicore.erp.architecture.validation.InputSanitizer;

import java.util.List;

public record CreateStandaloneDeliveryGuideRequest(
        @NotNull Long clientId,
        @NotNull Long warehouseId,
        @Size(max = 120, message = "O responsável deve ter no máximo 120 caracteres.")
        String responsible,
        @Size(max = 50, message = "O veículo deve ter no máximo 50 caracteres.")
        String vehicle,
        @Size(max = 500, message = "As observações não podem exceder 500 caracteres.")
        String notes,
        @NotEmpty(message = "Adicione pelo menos um produto à guia.")
        List<@Valid CreateStandaloneDeliveryGuideLineRequest> lines
) {
    public CreateStandaloneDeliveryGuideRequest {
        responsible = InputSanitizer.sanitizeText(responsible);
        vehicle = InputSanitizer.sanitizeText(vehicle);
        notes = InputSanitizer.sanitizeNotes(notes, 500);
        lines = lines == null ? List.of() : List.copyOf(lines);
    }
}
