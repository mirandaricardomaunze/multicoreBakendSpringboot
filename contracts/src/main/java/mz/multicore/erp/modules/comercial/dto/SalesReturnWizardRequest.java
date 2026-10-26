package mz.multicore.erp.modules.comercial.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Pedido completo de devolução assistida de uma fatura para geração de Nota de Crédito.
 */
public record SalesReturnWizardRequest(
        @NotNull @Positive Long invoiceId,
        @Size(max = 120) String reason,
        @NotNull @Positive Long warehouseId,
        boolean autoApprove,
        @Size(max = 1000) String description,
        @NotEmpty @Size(max = 1000) List<@Valid SalesReturnWizardLineRequest> lines
) {
    public SalesReturnWizardRequest {
        if (reason == null || reason.isBlank()) reason = "RETURN";
        if (lines == null) lines = List.of();
    }
}
