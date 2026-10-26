package mz.multicore.erp.modules.comercial.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CompleteOrderPickingRequest(
    @NotNull @Positive Long orderId,
    @NotBlank @Size(max = 120) String terminalName,
    @Size(max = 1000) String notes,
    @NotNull @Size(max = 1000) List<@Valid OrderPickingConfirmationLineRequest> lines
) {
    public CompleteOrderPickingRequest(Long orderId, String terminalName) {
        this(orderId, terminalName, null, List.of());
    }
}
