package mz.multicore.erp.modules.recurring.dto;

import mz.multicore.erp.modules.recurring.model.BillingFrequency;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pedido de registo de novo contrato recorrente.
 */
public record CreateRecurringContractRequest(
        @NotBlank(message = "O número do contrato é obrigatório.")
        @Size(max = 32) String contractNumber,

        @NotNull(message = "O cliente é obrigatório.")
        Long customerId,

        @NotBlank(message = "A descrição do serviço/avença é obrigatória.")
        @Size(max = 250) String description,

        @NotNull(message = "A frequência de facturação é obrigatória.")
        BillingFrequency frequency,

        @Min(value = 1, message = "O dia de facturação deve ser entre 1 e 31.")
        @Max(value = 31, message = "O dia de facturação deve ser entre 1 e 31.")
        int billingDayOfMonth,

        @NotNull(message = "O valor fixo da avença é obrigatório.")
        @Positive(message = "O valor deve ser positivo.")
        BigDecimal fixedAmount,

        @NotNull(message = "A data de início é obrigatória.")
        LocalDate startDate,

        LocalDate endDate,
        boolean autoRenew
) {}
