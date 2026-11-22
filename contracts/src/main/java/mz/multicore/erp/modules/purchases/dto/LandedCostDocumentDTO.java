package mz.multicore.erp.modules.purchases.dto;

import mz.multicore.erp.modules.purchases.model.LandedCostAllocationMethod;

import java.time.LocalDate;
import java.util.List;

/**
 * A "Custos de Importação" document (series CI), the purchases it is linked to in the order chosen, and its cost
 * lines with the debt each created to its supplier, and the effect on each purchase line (stock part and sold part).
 */
public record LandedCostDocumentDTO(
        Long id,
        String documentNumber,
        LocalDate documentDate,
        LandedCostAllocationMethod allocationMethod,
        List<Long> purchaseIds,
        List<String> purchaseNumbers,
        List<LandedCostChargeDTO> charges,
        List<LandedCostAllocationDTO> allocations
) {}
