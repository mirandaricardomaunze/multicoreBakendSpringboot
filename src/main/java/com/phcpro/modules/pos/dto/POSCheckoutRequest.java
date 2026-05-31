package com.phcpro.modules.pos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record POSCheckoutRequest(
        @NotBlank(message = "Operador é obrigatório.") String operator,
        @NotNull(message = "Empresa é obrigatória.") Long companyId,
        @NotNull(message = "Cliente é obrigatório.") Long clientId,
        @NotNull(message = "Armazém é obrigatório.") Long warehouseId,
        /** Compat antigo: pagamento único em conta de tesouraria. Use {@link #payments()} para multi-método. */
        Long treasuryAccountId,
        @NotEmpty(message = "A venda deve conter pelo menos uma linha.") @Valid List<POSCheckoutLineRequest> lines,
        /** Multi-método: lista de pagamentos (CASH/CARD/BANK_TRANSFER/CREDIT). Se vazio, usa-se treasuryAccountId. */
        @Valid List<PosPaymentRequest> payments
) {}
