package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.purchases.model.DocumentCurrency;
import mz.multicore.erp.modules.purchases.model.LandedCostCharge;
import mz.multicore.erp.modules.purchases.model.LandedCostType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Harness LC-03..05 (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4.2, §4.3).
 *
 * <p>LC-03: entram no custo FRETE, SEGURO, DIREITOS_ADUANEIROS, DESPACHANTE e OUTRO, sempre sem IVA e
 * convertidos à moeda base; IVA_ADUANEIRO e o IVA dos serviços vão a IVA dedutível, fora do custo.
 */
class LandedCostAllocationTest {

    private static final DocumentCurrency MZN = new DocumentCurrency("MZN", DocumentCurrency.BASE_RATE);
    private static final DocumentCurrency USD = new DocumentCurrency("USD", new BigDecimal("63.5000"));

    private final LandedCostCalculator calculator = new LandedCostCalculator();

    @Test
    void lc03_capitalizableTypesEnterCostAndCustomsVatDoesNot() {
        assertThat(LandedCostType.FRETE.capitalizable()).isTrue();
        assertThat(LandedCostType.SEGURO.capitalizable()).isTrue();
        assertThat(LandedCostType.DIREITOS_ADUANEIROS.capitalizable()).isTrue();
        assertThat(LandedCostType.DESPACHANTE.capitalizable()).isTrue();
        assertThat(LandedCostType.OUTRO.capitalizable()).isTrue();
        assertThat(LandedCostType.IVA_ADUANEIRO.capitalizable()).isFalse();
    }

    @Test
    void lc03_costIsNetOfVatInBaseCurrencyAndVatGoesToDeductible() {
        List<LandedCostCharge> charges = List.of(
                // frete 1 000 USD sem IVA → 63 500,00 MZN de custo
                new LandedCostCharge(LandedCostType.FRETE, USD, new BigDecimal("1000.00"), null),
                // despachante 10 000 MZN + IVA 16 % → 10 000 ao custo, 1 600 a IVA dedutível
                new LandedCostCharge(LandedCostType.DESPACHANTE, MZN, new BigDecimal("10000.00"), new BigDecimal("16")),
                // direitos 5 000 MZN
                new LandedCostCharge(LandedCostType.DIREITOS_ADUANEIROS, MZN, new BigDecimal("5000.00"), null),
                // IVA aduaneiro 12 000 MZN → todo a IVA dedutível, nada ao custo
                new LandedCostCharge(LandedCostType.IVA_ADUANEIRO, MZN, new BigDecimal("12000.00"), null));

        LandedCostCalculator.Totals totals = calculator.totals(charges);

        assertThat(totals.capitalizableCost()).isEqualByComparingTo("78500.00");
        assertThat(totals.deductibleVat()).isEqualByComparingTo("13600.00");
        assertThat(totals.payableTotal()).isEqualByComparingTo("92100.00");
    }

    @Test
    void lc03_serviceVatInForeignCurrencyIsConvertedAndKeptOutOfCost() {
        LandedCostCharge freight = new LandedCostCharge(
                LandedCostType.FRETE, USD, new BigDecimal("200.00"), new BigDecimal("16"));

        assertThat(freight.documentVatAmount()).isEqualByComparingTo("32.00");
        assertThat(freight.documentTotalAmount()).isEqualByComparingTo("232.00");
        assertThat(freight.baseCostAmount()).isEqualByComparingTo("12700.00");
        assertThat(freight.baseVatAmount()).isEqualByComparingTo("2032.00");
        assertThat(freight.basePayableAmount()).isEqualByComparingTo("14732.00");
    }

    @Test
    void lc03_customsVatIgnoresAnyVatRate() {
        LandedCostCharge customsVat = new LandedCostCharge(
                LandedCostType.IVA_ADUANEIRO, MZN, new BigDecimal("800.00"), new BigDecimal("16"));

        assertThat(customsVat.baseCostAmount()).isEqualByComparingTo("0");
        assertThat(customsVat.baseVatAmount()).isEqualByComparingTo("800.00");
        assertThat(customsVat.basePayableAmount()).isEqualByComparingTo("800.00");
    }

    @Test
    void lc03_invalidChargesAreRejected() {
        assertThrows(BusinessRuleException.class, () ->
                new LandedCostCharge(LandedCostType.FRETE, MZN, BigDecimal.ZERO, null));
        assertThrows(BusinessRuleException.class, () ->
                new LandedCostCharge(LandedCostType.FRETE, MZN, new BigDecimal("10"), new BigDecimal("-1")));
        assertThrows(BusinessRuleException.class, () ->
                new LandedCostCharge(null, MZN, new BigDecimal("10"), null));
    }
}
