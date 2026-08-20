package mz.multicore.erp.modules.comercial.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Nova data de validade de uma cotação. Data explícita (e não "mais N dias") porque é assim que a
 * concessão é acordada com o cliente — "estendo-lhe o preço até dia 30".
 */
public record ExtendQuotationValidityRequest(
        @NotNull(message = "A nova data de validade é obrigatória.")
        LocalDate validUntil
) {}
