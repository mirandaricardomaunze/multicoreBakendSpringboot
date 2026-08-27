package mz.multicore.erp.architecture.quantity;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LogisticsLoadCalculatorHarnessTest {
    @Test void calculatesWeightAndBothShares() {
        var result = LogisticsLoadCalculator.calculate(List.of(
                new LogisticsLoadCalculator.Input(new BigDecimal("10"), new BigDecimal("2")),
                new LogisticsLoadCalculator.Input(new BigDecimal("30"), new BigDecimal("1"))));
        assertEquals(new BigDecimal("20.000"), result.get(0).lineWeightKg());
        assertEquals(new BigDecimal("25.00"), result.get(0).quantityPercentage());
        assertEquals(new BigDecimal("40.00"), result.get(0).weightPercentage());
    }
    @Test void rejectsGrossWeightBelowNetWeight() {
        assertThrows(BusinessRuleException.class, () -> LogisticsLoadCalculator.validateWeights(
                new BigDecimal("2"), new BigDecimal("1.9")));
    }
}
