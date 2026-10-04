package mz.multicore.erp.modules.comercial.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Actualização integral de uma cotação ainda em rascunho. */
public record UpdateQuotationRequest(
        @NotNull(message = "A versão da cotação é obrigatória.") Long version,
        Long clientId,
        @Size(max = 120, message = "Nome do comprador deve ter no máximo 120 caracteres.")
        String walkInName,
        @NotNull(message = "O ID do armazém é obrigatório.") Long warehouseId,
        @Positive(message = "A validade deve ser de pelo menos um dia.") Integer validityDays,
        @Size(max = 200, message = "Condições de pagamento devem ter no máximo 200 caracteres.")
        String paymentTerms,
        @Size(max = 200, message = "Prazo de entrega deve ter no máximo 200 caracteres.")
        String deliveryTerms,
        @Positive(message = "O prazo de entrega deve ser de pelo menos um dia.") Integer deliveryDays,
        @Size(max = 1000, message = "Observações devem ter no máximo 1000 caracteres.")
        String notes,
        @NotEmpty(message = "A cotação deve conter pelo menos uma linha.") @Valid
        List<CreateQuotationLineRequest> lines
) {
    public UpdateQuotationRequest {
        walkInName = mz.multicore.erp.architecture.validation.InputSanitizer.sanitizeText(walkInName);
        paymentTerms = mz.multicore.erp.architecture.validation.InputSanitizer.sanitizeText(paymentTerms);
        deliveryTerms = mz.multicore.erp.architecture.validation.InputSanitizer.sanitizeText(deliveryTerms);
        notes = mz.multicore.erp.architecture.validation.InputSanitizer.sanitizeNotes(notes, 1000);
    }
}
