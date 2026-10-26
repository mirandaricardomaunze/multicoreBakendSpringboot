package mz.multicore.erp.modules.pos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Uma linha de pagamento submetida no checkout.
 * - {@code method}: CASH / CARD / BANK_TRANSFER / CREDIT
 * - {@code amount}: valor que esta entrada cobre
 * - {@code tenderedAmount}: para CASH, o valor que o cliente entregou (≥ amount); o troco é calculado pelo service
 * - {@code reference}: nº de autorização do cartão ou comprovativo de transferência
 * - {@code treasuryAccountId}: conta de tesouraria a movimentar (obrigatório para CASH/CARD/BANK_TRANSFER; null para CREDIT)
 */
public record PosPaymentRequest(
        @NotBlank String method,
        @NotNull @Positive BigDecimal amount,
        @PositiveOrZero BigDecimal tenderedAmount,
        String reference,
        Long treasuryAccountId,
        @Size(max = 60, message = "A chave de idempotência não pode exceder 60 caracteres.")
        String idempotencyKey
) {
    public PosPaymentRequest {
        idempotencyKey = idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim();
    }

    /** Construtor retrocompatível anterior à protecção de pagamentos posteriores. */
    public PosPaymentRequest(String method, BigDecimal amount, BigDecimal tenderedAmount,
                             String reference, Long treasuryAccountId) {
        this(method, amount, tenderedAmount, reference, treasuryAccountId, null);
    }
}
