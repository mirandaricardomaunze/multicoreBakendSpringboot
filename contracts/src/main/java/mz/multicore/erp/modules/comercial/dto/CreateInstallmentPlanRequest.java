package mz.multicore.erp.modules.comercial.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Pedido de criação ou reestruturação de plano de vencimentos fracionados.
 */
public record CreateInstallmentPlanRequest(
        @NotNull @Positive Long invoiceId,
        @Min(1) @Max(120) int numberOfInstallments,
        @Min(1) @Max(3650) int intervalDays,
        LocalDate firstDueDate,
        @Size(max = 120) List<@NotNull @Positive BigDecimal> customAmounts
) {
    public CreateInstallmentPlanRequest {
        if (firstDueDate == null) firstDueDate = LocalDate.now().plusDays(intervalDays);
        if (customAmounts == null) customAmounts = List.of();
    }
}
