package mz.multicore.erp.modules.bulkstock.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.bulkstock.model.BulkMeasurementMethod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** SA-C17: pure bulk-pile rules ({@code docs/STOCK_GRANEL_SPEC.md}). */
class BulkMeasurementRulesTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void pileVolumes() {
        assertThat(BulkMeasurementRules.volume(BulkMeasurementMethod.BOX, bd("10"), bd("5"), bd("2"))).isEqualByComparingTo("100");
        assertThat(BulkMeasurementRules.volume(BulkMeasurementMethod.HEAP, bd("10"), bd("5"), bd("2"))).isEqualByComparingTo("50");
        // Cone with a 6 m base and 3 m height: pi * 3^2 * 3 / 3 = 28.274 m3.
        assertThat(BulkMeasurementRules.volume(BulkMeasurementMethod.CONE, bd("6"), bd("3"), null)).isEqualByComparingTo("28.274");
    }

    @Test
    void rejectsMissingAbsurdOrDirectDimensions() {
        assertThatThrownBy(() -> BulkMeasurementRules.volume(BulkMeasurementMethod.BOX, bd("10"), null, bd("2")))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("largura");
        assertThatThrownBy(() -> BulkMeasurementRules.volume(BulkMeasurementMethod.BOX, bd("10"), bd("5"), bd("2000")))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("engano");
        assertThatThrownBy(() -> BulkMeasurementRules.volume(BulkMeasurementMethod.DIRECT, bd("1"), bd("1"), bd("1")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void differenceDeductsSoldMaterialStillOnThePile() {
        assertThat(BulkMeasurementRules.difference(bd("48"), bd("10"), bd("38"))).isEqualByComparingTo("0");
        assertThat(BulkMeasurementRules.difference(bd("45"), bd("10"), bd("38"))).isEqualByComparingTo("-3");
    }

    @Test
    void percentAndTolerance() {
        assertThat(BulkMeasurementRules.differencePercent(bd("-2"), bd("50"))).isEqualByComparingTo("4");
        assertThat(BulkMeasurementRules.differencePercent(bd("0"), bd("0"))).isEqualByComparingTo("0");
        assertThat(BulkMeasurementRules.differencePercent(bd("5"), bd("0"))).isEqualByComparingTo("100");
        assertThat(BulkMeasurementRules.withinTolerance(bd("5.00"), bd("5"))).isTrue();
        assertThat(BulkMeasurementRules.withinTolerance(bd("5.01"), bd("5"))).isFalse();
        assertThatThrownBy(() -> BulkMeasurementRules.requireTolerance(bd("60"))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> BulkMeasurementRules.requireTolerance(null)).isInstanceOf(BusinessRuleException.class);
    }
}
