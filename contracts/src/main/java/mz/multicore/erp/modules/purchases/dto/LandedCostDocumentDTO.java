package mz.multicore.erp.modules.purchases.dto;

import mz.multicore.erp.modules.purchases.model.LandedCostAllocationMethod;

import java.time.LocalDate;
import java.util.List;

/** A "Custos de Importação" document (series CI) and the purchases it is linked to, in the order chosen. */
public record LandedCostDocumentDTO(
        Long id,
        String documentNumber,
        LocalDate documentDate,
        LandedCostAllocationMethod allocationMethod,
        List<Long> purchaseIds,
        List<String> purchaseNumbers
) {}
