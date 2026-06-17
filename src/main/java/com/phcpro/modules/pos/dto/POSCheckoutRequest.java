package com.phcpro.modules.pos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record POSCheckoutRequest(
        @NotBlank(message = "Operador é obrigatório.") String operator,
        @NotNull(message = "Empresa é obrigatória.") Long companyId,
        /** Opcional. Se nulo, a venda é registada para "Consumidor Final" (balcão). */
        Long clientId,
        /** Opcional, só relevante quando {@code clientId} é nulo. Nome livre escrito pelo operador
         *  para identificar o comprador no recibo, sem criar registo de cliente. */
        @Size(max = 120, message = "Nome do comprador deve ter no máximo 120 caracteres.")
        String walkInName,
        @NotNull(message = "Armazém é obrigatório.") Long warehouseId,
        /** Compat antigo: pagamento único em conta de tesouraria. Use {@link #payments()} para multi-método. */
        Long treasuryAccountId,
        @NotEmpty(message = "A venda deve conter pelo menos uma linha.") @Valid List<POSCheckoutLineRequest> lines,
        /** Multi-método: lista de pagamentos (CASH/CARD/BANK_TRANSFER/CREDIT). Se vazio, usa-se treasuryAccountId. */
        @Valid List<PosPaymentRequest> payments
) {}
