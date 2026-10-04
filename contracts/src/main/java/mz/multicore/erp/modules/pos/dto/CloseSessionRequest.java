package mz.multicore.erp.modules.pos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CloseSessionRequest(
        @NotNull(message = "Saldo real de fecho é obrigatório.")
        @PositiveOrZero(message = "Saldo real não pode ser negativo.") BigDecimal closingBalanceReal,

        // Opcional: conta de tesouraria que recebe o depósito do numerário da sessão.
        // Se null, a sessão fecha sem gerar o depósito automático.
        Long depositAccountId,

        // Opcional: notas / justificação de quebra ou sobra do fecho cego
        @jakarta.validation.constraints.Size(max = 500, message = "As notas de fecho não podem exceder 500 caracteres.")
        String notes,

        // Opcional: JSON com a contagem das notas e moedas
        @jakarta.validation.constraints.Size(max = 5000, message = "O detalhe de contagem excede o limite permitido.")
        String cashBreakdownJson
) {
    public CloseSessionRequest {
        notes = mz.multicore.erp.architecture.validation.InputSanitizer.sanitizeNotes(notes, 500);
        cashBreakdownJson = mz.multicore.erp.architecture.validation.InputSanitizer.stripControlCharacters(cashBreakdownJson);
    }

    public CloseSessionRequest(BigDecimal closingBalanceReal, Long depositAccountId) {
        this(closingBalanceReal, depositAccountId, null, null);
    }
}
