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

    @Test void recommendsVehicleAccordingToWeight() {
        assertEquals("Veículo Ligeiro / Pick-up (até 1 Ton)",
                LogisticsLoadCalculator.recommendedVehicleType(new BigDecimal("850")));
        assertEquals("Comercial Ligeiro / Carrinha 3.5T (ex: Canter / Dyna / Isuzu)",
                LogisticsLoadCalculator.recommendedVehicleType(new BigDecimal("2400")));
        assertEquals("Camião Médio (até 10 Ton - ex: Isuzu FTR / Hino 500)",
                LogisticsLoadCalculator.recommendedVehicleType(new BigDecimal("6500")));
        assertEquals("Camião Pesado (até 28 Ton - Trucado / 3 Eixos)",
                LogisticsLoadCalculator.recommendedVehicleType(new BigDecimal("18000")));
        assertEquals("Carga Excedente / Requer Articulado Pesado ou Múltiplos Carros",
                LogisticsLoadCalculator.recommendedVehicleType(new BigDecimal("35000")));
    }

    @Test void formatsWeightWithVehicleRecommendation() {
        String formatted = LogisticsLoadCalculator.formatWeightWithVehicle(new BigDecimal("2500"));
        assertTrue(formatted.contains("2500 kg (2.5 Ton)"));
        assertTrue(formatted.contains("Carrinha 3.5T"));
    }

    @Test void rejectsGrossWeightBelowNetWeight() {
        assertThrows(BusinessRuleException.class, () -> LogisticsLoadCalculator.validateWeights(
                new BigDecimal("2"), new BigDecimal("1.9")));
    }
}
