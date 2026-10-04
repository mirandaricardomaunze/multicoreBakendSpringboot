package mz.multicore.erp.modules.pos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Pedido de passagem de turno (handover) entre operadores na mesma sessão de caixa.
 *
 * <p>As regras de negócio (operadores diferentes, operador de saída coincide com o da sessão,
 * sessão aberta) continuam no {@code POSService}; aqui só se barra o que é malformado.
 */
public record ShiftHandoverRequest(
    @NotNull(message = "A sessão de caixa é obrigatória.")
    Long sessionId,

    @Size(max = 100, message = "O nome do operador de saída não pode exceder 100 caracteres.")
    String outgoingOperator,

    @NotBlank(message = "O operador de entrada é obrigatório.")
    @Size(max = 100, message = "O nome do operador de entrada não pode exceder 100 caracteres.")
    String incomingOperator,

    @PositiveOrZero(message = "O valor contado não pode ser negativo.")
    BigDecimal countedCash,

    @Size(max = 5000, message = "O detalhe da contagem excede o tamanho permitido.")
    String cashBreakdownJson,

    @Size(max = 500, message = "As observações não podem exceder 500 caracteres.")
    String notes
) {
    public ShiftHandoverRequest {
        outgoingOperator = mz.multicore.erp.architecture.validation.InputSanitizer.sanitizeText(outgoingOperator);
        incomingOperator = mz.multicore.erp.architecture.validation.InputSanitizer.sanitizeText(incomingOperator);
        notes = mz.multicore.erp.architecture.validation.InputSanitizer.sanitizeNotes(notes, 500);
        cashBreakdownJson = mz.multicore.erp.architecture.validation.InputSanitizer.stripControlCharacters(cashBreakdownJson);
    }
}
