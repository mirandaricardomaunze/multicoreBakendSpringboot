package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.modules.purchases.model.LandedCostCharge;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cálculo puro dos custos de importação (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4.2, §4.3): soma, em moeda
 * base, o que entra no custo dos artigos e o que vai a IVA dedutível. Sem estado nem persistência, para que a
 * pré-visualização e a confirmação usem exactamente as mesmas contas.
 */
public class LandedCostCalculator {

    /** Totais em moeda base de um documento CI. */
    public record Totals(BigDecimal capitalizableCost, BigDecimal deductibleVat) {

        /** Total das dívidas aos fornecedores do documento, em moeda base. */
        public BigDecimal payableTotal() {
            return capitalizableCost.add(deductibleVat);
        }
    }

    public Totals totals(List<LandedCostCharge> charges) {
        BigDecimal cost = BigDecimal.ZERO.setScale(2);
        BigDecimal vat = BigDecimal.ZERO.setScale(2);
        for (LandedCostCharge charge : charges) {
            cost = cost.add(charge.baseCostAmount());
            vat = vat.add(charge.baseVatAmount());
        }
        return new Totals(cost, vat);
    }
}
