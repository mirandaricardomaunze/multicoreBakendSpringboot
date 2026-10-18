package mz.multicore.erp.gui.tv;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoalProgressBarTest {

    @Test
    @DisplayName("GoalProgressBar calcula percentagem correctamente")
    void calculatesPercentageCorrectly() {
        GoalProgressBar bar = new GoalProgressBar();

        // 50%
        bar.setValues(new BigDecimal("500000.00"), new BigDecimal("1000000.00"));
        assertEquals(50.0, bar.getPercentage(), 0.01);

        // 83.3%
        bar.setValues(new BigDecimal("250000.00"), new BigDecimal("300000.00"));
        assertEquals(83.3, bar.getPercentage(), 0.05);

        // 110% (superada)
        bar.setValues(new BigDecimal("1100000.00"), new BigDecimal("1000000.00"));
        assertEquals(110.0, bar.getPercentage(), 0.01);

        // Meta zero ou negativa
        bar.setValues(new BigDecimal("100.00"), BigDecimal.ZERO);
        assertEquals(0.0, bar.getPercentage(), 0.01);
    }

    @Test
    @DisplayName("BusinessUnitGoalManager armazena e recupera metas padrão e personalizadas")
    void businessUnitGoalManager_storesAndRetrieves() {
        BigDecimal initialGlobal = BusinessUnitGoalManager.getGlobalGoal();
        assertTrue(initialGlobal != null && initialGlobal.compareTo(BigDecimal.ZERO) >= 0);

        BigDecimal custom = new BigDecimal("7500000.00");
        BusinessUnitGoalManager.setGlobalGoal(custom);
        assertEquals(custom, BusinessUnitGoalManager.getGlobalGoal());

        // Restaurar
        BusinessUnitGoalManager.setGlobalGoal(initialGlobal);

        // Por unidade
        BigDecimal unitGoal = new BigDecimal("1250000.00");
        BusinessUnitGoalManager.setGoalForUnit(999L, unitGoal);
        assertEquals(unitGoal, BusinessUnitGoalManager.getGoalForUnit(999L));
    }

    @Test
    @DisplayName("BusinessUnitGoalManager armazena seleção modular de unidades incluídas")
    void businessUnitGoalManager_unitInclusionSelection() {
        Long testUnitId = 8888L;
        // Padrão é true
        assertTrue(BusinessUnitGoalManager.isUnitIncluded(testUnitId));

        // Desmarcar unidade
        BusinessUnitGoalManager.setUnitIncluded(testUnitId, false);
        org.junit.jupiter.api.Assertions.assertFalse(BusinessUnitGoalManager.isUnitIncluded(testUnitId));

        // Marcar novamente
        BusinessUnitGoalManager.setUnitIncluded(testUnitId, true);
        assertTrue(BusinessUnitGoalManager.isUnitIncluded(testUnitId));
    }
}
