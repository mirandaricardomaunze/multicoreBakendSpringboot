package mz.multicore.erp.modules.comercial.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Liquidação de uma fatura: conta de tesouraria, método e valor pago. */
public record CreateReceiptRequest(
        @NotNull(message = "Fatura é obrigatória.") Long invoiceId,
        @NotNull(message = "Conta de tesouraria é obrigatória.") Long treasuryAccountId,
        @NotNull(message = "Método de pagamento é obrigatório.") String paymentMethod,
        @NotNull(message = "Valor pago é obrigatório.")
        @PositiveOrZero(message = "Valor pago não pode ser negativo.") BigDecimal amountPaid
) {}
