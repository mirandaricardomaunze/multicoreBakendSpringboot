package mz.multicore.erp.modules.productionplan;

import mz.multicore.erp.modules.productionplan.service.ProductionPlanRules;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionPlanRulesTest {

    @Test
    void ingredientsScaleWithTheRecipeAndIncludeScrap() {
        // Recipe: 100 breads need 5 kg of flour with 2% scrap.
        assertThat(ProductionPlanRules.ingredient(new BigDecimal("60"), new BigDecimal("100"), new BigDecimal("5"), new BigDecimal("2")))
                .isEqualByComparingTo("3.06");
        assertThat(ProductionPlanRules.ingredient(BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.ONE, null)).isEqualByComparingTo("0");
    }

    @Test
    void suggestionAveragesTheSameWeekdayAndRoundsUp() {
        LocalDate saturday = LocalDate.of(2026, 11, 14);
        Map<LocalDate, BigDecimal> sold = Map.of(
                saturday.minusWeeks(1), new BigDecimal("120"),
                saturday.minusWeeks(2), new BigDecimal("100"),
                saturday.minusWeeks(3), new BigDecimal("101"),
                saturday.minusDays(1), new BigDecimal("999"));
        // (120 + 100 + 101 + 0) / 4 = 80.25 -> 81: a week without sales counts as zero, Friday does not count.
        assertThat(ProductionPlanRules.suggestion(sold, saturday, 4)).isEqualByComparingTo("81");
    }

    @Test
    void remainingNeverGoesBelowZero() {
        assertThat(ProductionPlanRules.remaining(new BigDecimal("100"), new BigDecimal("60"))).isEqualByComparingTo("40");
        assertThat(ProductionPlanRules.remaining(new BigDecimal("100"), new BigDecimal("120"))).isEqualByComparingTo("0");
    }
}
