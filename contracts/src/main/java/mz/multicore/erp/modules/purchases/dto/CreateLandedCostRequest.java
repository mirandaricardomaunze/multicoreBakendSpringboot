package mz.multicore.erp.modules.purchases.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import mz.multicore.erp.modules.purchases.model.LandedCostAllocationMethod;

import java.time.LocalDate;
import java.util.List;

/**
 * New "Custos de Importação" document: the received purchases it applies to, the allocation method (VALOR when
 * omitted), the document date (today when omitted) and the cost lines, each owed to its own supplier.
 */
public record CreateLandedCostRequest(
        @NotEmpty(message = "Escolha pelo menos uma compra recebida.") List<Long> purchaseIds,
        LandedCostAllocationMethod allocationMethod,
        LocalDate documentDate,
        @NotEmpty(message = "Indique pelo menos uma linha de custo.") List<@Valid LandedCostChargeRequest> charges
) {}
