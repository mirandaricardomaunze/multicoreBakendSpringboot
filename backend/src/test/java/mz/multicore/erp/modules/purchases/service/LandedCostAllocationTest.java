package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.purchases.model.DocumentCurrency;
import mz.multicore.erp.modules.purchases.model.LandedCostAllocationMethod;
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
 *
 * <p>LC-04: o custo reparte-se pelas linhas de compra por VALOR (omissão), PESO
 * ({@code netUnitWeightKg × quantidade}; linha sem peso é erro) ou QUANTIDADE.
 *
 * <p>LC-05: quotas a 2 casas HALF_UP; o resíduo de cêntimos vai para a maior quota (empate: a primeira),
 * para a soma repartida ser exactamente o total.
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

    // ---- LC-04: repartição por VALOR (omissão), PESO ou QUANTIDADE ----

    private static final List<LandedCostCalculator.AllocationLine> LINES = List.of(
            // linha 1: 10 un. de 3 kg, valor 60 000 MZN → peso 30 kg
            new LandedCostCalculator.AllocationLine(1L, new BigDecimal("60000.00"), new BigDecimal("10"), new BigDecimal("3")),
            // linha 2: 30 un. de 0,5 kg, valor 20 000 MZN → peso 15 kg
            new LandedCostCalculator.AllocationLine(2L, new BigDecimal("20000.00"), new BigDecimal("30"), new BigDecimal("0.5")),
            // linha 3: 10 un. de 0,5 kg, valor 20 000 MZN → peso 5 kg
            new LandedCostCalculator.AllocationLine(3L, new BigDecimal("20000.00"), new BigDecimal("10"), new BigDecimal("0.5")));

    @Test
    void lc04_byValueIsTheDefault() {
        List<LandedCostCalculator.Allocation> byValue =
                calculator.allocate(new BigDecimal("10000.00"), LandedCostAllocationMethod.VALOR, LINES);
        List<LandedCostCalculator.Allocation> byDefault =
                calculator.allocate(new BigDecimal("10000.00"), null, LINES);

        assertThat(byValue).extracting(LandedCostCalculator.Allocation::lineId).containsExactly(1L, 2L, 3L);
        assertThat(byValue).extracting(LandedCostCalculator.Allocation::share)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("6000.00"), new BigDecimal("2000.00"), new BigDecimal("2000.00"));
        assertThat(byDefault).isEqualTo(byValue);
    }

    @Test
    void lc04_byWeightUsesNetUnitWeightTimesQuantity() {
        List<LandedCostCalculator.Allocation> shares =
                calculator.allocate(new BigDecimal("10000.00"), LandedCostAllocationMethod.PESO, LINES);

        assertThat(shares).extracting(LandedCostCalculator.Allocation::share)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("6000.00"), new BigDecimal("3000.00"), new BigDecimal("1000.00"));
    }

    @Test
    void lc04_byQuantity() {
        List<LandedCostCalculator.Allocation> shares =
                calculator.allocate(new BigDecimal("10000.00"), LandedCostAllocationMethod.QUANTIDADE, LINES);

        assertThat(shares).extracting(LandedCostCalculator.Allocation::share)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("2000.00"), new BigDecimal("6000.00"), new BigDecimal("2000.00"));
    }

    @Test
    void lc04_byWeightWithALineWithoutWeightIsAnError() {
        List<LandedCostCalculator.AllocationLine> lines = List.of(
                new LandedCostCalculator.AllocationLine(1L, new BigDecimal("100.00"), new BigDecimal("2"), new BigDecimal("1")),
                new LandedCostCalculator.AllocationLine(2L, new BigDecimal("100.00"), new BigDecimal("2"), null));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                calculator.allocate(new BigDecimal("50.00"), LandedCostAllocationMethod.PESO, lines));
        assertThat(ex.getMessage()).contains("peso");
        // os outros métodos não precisam do peso
        assertThat(calculator.allocate(new BigDecimal("50.00"), LandedCostAllocationMethod.QUANTIDADE, lines))
                .hasSize(2);
    }

    @Test
    void lc04_withoutLinesOrBasisIsAnError() {
        assertThrows(BusinessRuleException.class, () ->
                calculator.allocate(new BigDecimal("50.00"), LandedCostAllocationMethod.VALOR, List.of()));
        assertThrows(BusinessRuleException.class, () -> calculator.allocate(new BigDecimal("50.00"),
                LandedCostAllocationMethod.VALOR,
                List.of(new LandedCostCalculator.AllocationLine(1L, BigDecimal.ZERO, BigDecimal.ONE, null))));
    }

    // ---- LC-05: 2 casas, resíduo de cêntimos na maior quota, soma exactamente o total ----

    private static LandedCostCalculator.AllocationLine qty(long id, String quantity) {
        return new LandedCostCalculator.AllocationLine(id, null, new BigDecimal(quantity), null);
    }

    private static BigDecimal sum(List<LandedCostCalculator.Allocation> allocations) {
        return allocations.stream().map(LandedCostCalculator.Allocation::share)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    void lc05_excessCentIsRemovedFromTheLargestShare() {
        // 1,00 por 1:1:4 → 0,1667 / 0,1667 / 0,6667 → 0,17 + 0,17 + 0,67 = 1,01; o cêntimo a mais sai da maior
        List<LandedCostCalculator.Allocation> shares = calculator.allocate(new BigDecimal("1.00"),
                LandedCostAllocationMethod.QUANTIDADE, List.of(qty(1L, "1"), qty(2L, "1"), qty(3L, "4")));

        assertThat(shares).extracting(LandedCostCalculator.Allocation::share)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("0.17"), new BigDecimal("0.17"), new BigDecimal("0.66"));
        assertThat(sum(shares)).isEqualByComparingTo("1.00");
    }

    @Test
    void lc05_missingCentGoesToTheLargestShareAndTiesGoToTheFirst() {
        // 100,00 por 1:1:1 → 33,33 × 3 = 99,99; o cêntimo em falta vai à primeira das maiores
        List<LandedCostCalculator.Allocation> shares = calculator.allocate(new BigDecimal("100.00"),
                LandedCostAllocationMethod.QUANTIDADE, List.of(qty(1L, "1"), qty(2L, "1"), qty(3L, "1")));

        assertThat(shares).extracting(LandedCostCalculator.Allocation::share)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("33.34"), new BigDecimal("33.33"), new BigDecimal("33.33"));
        assertThat(sum(shares)).isEqualByComparingTo("100.00");
    }

    @Test
    void lc05_sharesAlwaysSumExactlyToTheTotalWithTwoDecimals() {
        List<LandedCostCalculator.AllocationLine> lines = List.of(
                qty(1L, "7"), qty(2L, "3"), qty(3L, "11"), qty(4L, "13"), qty(5L, "1"), qty(6L, "1"), qty(7L, "1"));
        for (String total : List.of("0.01", "0.05", "1.00", "999.99", "78500.00", "12345.67")) {
            List<LandedCostCalculator.Allocation> shares =
                    calculator.allocate(new BigDecimal(total), LandedCostAllocationMethod.QUANTIDADE, lines);

            assertThat(sum(shares)).as("total %s", total).isEqualByComparingTo(total);
            assertThat(shares).allSatisfy(a -> {
                assertThat(a.share().scale()).isEqualTo(2);
                assertThat(a.share().signum()).isGreaterThanOrEqualTo(0);
            });
        }
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
