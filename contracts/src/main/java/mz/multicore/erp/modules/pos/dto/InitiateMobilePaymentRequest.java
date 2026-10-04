package mz.multicore.erp.modules.pos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import mz.multicore.erp.architecture.validation.ValidPhoneMZ;

import java.math.BigDecimal;

/**
 * Pedido para envio de solicitação Push USSD ao telemóvel do cliente.
 */
public record InitiateMobilePaymentRequest(
        @NotNull(message = "O operador de pagamento móvel é obrigatório.")
        MobilePaymentProvider provider,

        @NotBlank(message = "O número de telemóvel é obrigatório.")
        @ValidPhoneMZ
        String phoneNumber,

        @NotNull(message = "O valor é obrigatório.")
        @Positive(message = "O valor deve ser positivo.")
        BigDecimal amount,

        @Size(max = 100, message = "A referência não pode exceder 100 caracteres.")
        String reference,

        Long companyId,

        @Size(max = 100, message = "O nome do operador não pode exceder 100 caracteres.")
        String operator
) {}
