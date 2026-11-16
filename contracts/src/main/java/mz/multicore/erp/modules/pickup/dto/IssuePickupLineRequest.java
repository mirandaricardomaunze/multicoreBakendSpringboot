package mz.multicore.erp.modules.pickup.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Quantity of one invoice line to hand over now, in the product base unit. */
public record IssuePickupLineRequest(
        @NotNull(message = "Linha da fatura é obrigatória.") Long invoiceLineId,
        @NotNull(message = "Quantidade é obrigatória.") BigDecimal quantity
) {}
