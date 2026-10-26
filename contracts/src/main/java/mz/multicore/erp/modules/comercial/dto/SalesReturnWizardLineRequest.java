package mz.multicore.erp.modules.comercial.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Linha individual a ser devolvida no assistente de devolução.
 */
public record SalesReturnWizardLineRequest(
        @NotNull @Positive Long invoiceLineId,
        @NotNull @Positive BigDecimal quantity,
        boolean restock
) {}
