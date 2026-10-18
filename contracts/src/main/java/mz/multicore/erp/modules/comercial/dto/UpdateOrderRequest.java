package mz.multicore.erp.modules.comercial.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Dados editáveis de uma encomenda existente; número e tipo permanecem imutáveis. */
public record UpdateOrderRequest(
        @NotNull(message = "A versão da encomenda é obrigatória.") Long version,
        Long clientId,
        @Size(max = 120, message = "Nome do comprador deve ter no máximo 120 caracteres.")
        String walkInName,
        @NotNull(message = "O ID do armazém é obrigatório.") Long warehouseId,
        Long destinationWarehouseId,
        @NotEmpty(message = "A encomenda deve conter pelo menos uma linha.") @Valid
        List<CreateInvoiceLineRequest> lines,
        @Size(max = 1000, message = "Observações devem ter no máximo 1000 caracteres.")
        String notes
) {
    public UpdateOrderRequest(Long version, Long clientId, String walkInName, Long warehouseId,
                             Long destinationWarehouseId, List<CreateInvoiceLineRequest> lines) {
        this(version, clientId, walkInName, warehouseId, destinationWarehouseId, lines, null);
    }
}
