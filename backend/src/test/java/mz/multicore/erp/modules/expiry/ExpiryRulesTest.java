package mz.multicore.erp.modules.expiry;

import mz.multicore.erp.modules.expiry.service.ExpiryRules;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExpiryRulesTest {

    private static final List<ExpiryRules.Rule> RULES = List.of(
            new ExpiryRules.Rule(null, 3, new BigDecimal("20")),
            new ExpiryRules.Rule(null, 1, new BigDecimal("50")),
            new ExpiryRules.Rule(7L, 2, new BigDecimal("30")));

    @Test
    void stagesFollowTheDaysLeft() {
        LocalDate today = LocalDate.of(2026, 11, 8);
        assertThat(ExpiryRules.stage(ExpiryRules.daysLeft(today, today.minusDays(1)))).isEqualTo("EXPIRED");
        assertThat(ExpiryRules.stage(0)).isEqualTo("TODAY");
        assertThat(ExpiryRules.stage(2)).isEqualTo("SOON");
        assertThat(ExpiryRules.stage(7)).isEqualTo("WEEK");
        assertThat(ExpiryRules.stage(8)).isEqualTo("LATER");
    }

    @Test
    void theLargestDiscountWhoseWindowStartedApplies() {
        assertThat(ExpiryRules.suggested(RULES, null, 5)).isNull();
        assertThat(ExpiryRules.suggested(RULES, null, 3)).isEqualByComparingTo("20");
        assertThat(ExpiryRules.suggested(RULES, null, 1)).isEqualByComparingTo("50");
        assertThat(ExpiryRules.suggested(RULES, null, -1)).as("expired lots are written off, not discounted").isNull();
    }

    @Test
    void categoryRulesTakePrecedenceOverGeneralRules() {
        assertThat(ExpiryRules.suggested(RULES, 7L, 2)).isEqualByComparingTo("30");
        assertThat(ExpiryRules.suggested(RULES, 7L, 3)).as("category window not started: general rule").isEqualByComparingTo("20");
        assertThat(ExpiryRules.suggested(RULES, 9L, 1)).isEqualByComparingTo("50");
    }
}
