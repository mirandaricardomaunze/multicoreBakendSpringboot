package mz.multicore.erp.modules.butchery.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BreakdownCostRulesTest {

    private static BreakdownCostRules.Cut cut(long id, String kg, String price) {
        return new BreakdownCostRules.Cut(id, new BigDecimal(kg), new BigDecimal(price));
    }

    @Test
    void costFollowsSaleValueSoFilletCostsMoreThanBones() {
        var result = BreakdownCostRules.allocate(new BigDecimal("50000"), List.of(
                cut(1, "20", "900"),   // fillet: value 18 000
                cut(2, "100", "420"),  // steak and stew: value 42 000
                cut(3, "40", "0")));   // bones: no value, no cost
        assertThat(result.get(0).cost()).isEqualByComparingTo("15000.00");
        assertThat(result.get(1).cost()).isEqualByComparingTo("35000.00");
        assertThat(result.get(2).cost()).isEqualByComparingTo("0");
        assertThat(result.get(0).costPerKg()).isGreaterThan(result.get(1).costPerKg());
    }

    @Test
    void sharesAlwaysAddUpToTheCarcassCost() {
        var result = BreakdownCostRules.allocate(new BigDecimal("1000"), List.of(
                cut(1, "1", "1"), cut(2, "1", "1"), cut(3, "1", "1")));
        BigDecimal sum = result.stream().map(BreakdownCostRules.Allocation::cost).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("1000.00");
    }

    @Test
    void withoutPricesTheCostIsSharedByWeight() {
        var result = BreakdownCostRules.allocate(new BigDecimal("300"), List.of(cut(1, "10", "0"), cut(2, "20", "0")));
        assertThat(result.get(0).cost()).isEqualByComparingTo("100.00");
        assertThat(result.get(1).cost()).isEqualByComparingTo("200.00");
    }

    @Test
    void cutsCannotWeighMoreThanTheCarcass() {
        assertThatThrownBy(() -> BreakdownCostRules.requireOutputWithinInput(new BigDecimal("100"), new BigDecimal("100.5")))
                .isInstanceOf(BusinessRuleException.class);
        BreakdownCostRules.requireOutputWithinInput(new BigDecimal("100"), new BigDecimal("100"));
    }

    @Test
    void lossAlertOnlyAboveTheTolerance() {
        assertThat(BreakdownCostRules.percentOf(new BigDecimal("12"), new BigDecimal("200"))).isEqualByComparingTo("6.00");
        assertThat(BreakdownCostRules.lossAboveTolerance(new BigDecimal("6"), new BigDecimal("8"))).isFalse();
        assertThat(BreakdownCostRules.lossAboveTolerance(new BigDecimal("9"), new BigDecimal("8"))).isTrue();
        assertThat(BreakdownCostRules.lossAboveTolerance(new BigDecimal("9"), null)).isFalse();
    }
}
