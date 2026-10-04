package mz.multicore.erp.modules.subscription.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import mz.multicore.erp.architecture.validation.ValidPhoneMZ;

/**
 * Pedido de renovação ou ativação de subscrição emitido pelo próprio cliente (self-service).
 *
 * <p>O {@code SubscriptionService} já assume 1 mês quando {@code months < 1}; o tecto evita que
 * um valor absurdo estoire em {@code LocalDate.plusMonths} e saia como erro interno.
 */
public record SelfServiceSubscriptionPaymentRequest(
        @Size(max = 30, message = "O plano não pode exceder 30 caracteres.")
        String plan,

        @Max(value = 120, message = "A renovação não pode exceder 120 meses.")
        int months,

        @Size(max = 30, message = "O método de pagamento não pode exceder 30 caracteres.")
        String paymentMethod,

        @ValidPhoneMZ
        String phoneNumber,

        @Size(max = 100, message = "A referência não pode exceder 100 caracteres.")
        String reference,

        @Size(max = 500, message = "As observações não podem exceder 500 caracteres.")
        String notes
) {}
