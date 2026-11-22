package mz.multicore.erp.gui.landedcost;

import mz.multicore.erp.modules.purchases.dto.LandedCostChargeRequest;

/**
 * A cost line typed in the "Custos de Importação" screen before the CI is confirmed: the request sent to the backend
 * plus the texts shown in the cost-lines table (supplier, rate and VAT), so the table never looks them up again.
 */
public record LandedCostChargeDraft(LandedCostChargeRequest request, String supplierName, String rateLabel,
                                    String vatLabel) {
}
