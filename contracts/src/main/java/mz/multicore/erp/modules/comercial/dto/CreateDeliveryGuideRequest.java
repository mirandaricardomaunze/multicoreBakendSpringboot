package mz.multicore.erp.modules.comercial.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mz.multicore.erp.architecture.validation.InputSanitizer;

/**
 * Pedido de criação de uma Guia de Remessa a partir de uma encomenda aprovada.
 * A empresa, o cliente, o armazém e as linhas derivam da encomenda — aqui só entra a origem
 * (encomenda) e os dados livres de transporte.
 */
public record CreateDeliveryGuideRequest(
        @NotNull Long orderId,
        @Size(max = 120, message = "O responsável deve ter no máximo 120 caracteres.")
        String responsible,
        @Size(max = 50, message = "O veículo deve ter no máximo 50 caracteres.")
        String vehicle,
        @Size(max = 500, message = "As observações não podem exceder 500 caracteres.")
        String notes
) {
    public CreateDeliveryGuideRequest {
        responsible = InputSanitizer.sanitizeText(responsible);
        vehicle = InputSanitizer.sanitizeText(vehicle);
        notes = InputSanitizer.sanitizeNotes(notes, 500);
    }
}
