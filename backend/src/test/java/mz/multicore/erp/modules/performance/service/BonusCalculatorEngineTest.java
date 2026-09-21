package mz.multicore.erp.modules.performance.service;

import mz.multicore.erp.modules.performance.model.BonusType;
import mz.multicore.erp.modules.performance.model.SalesGoal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BonusCalculatorEngineTest {

    private BonusCalculatorEngine engine;

    @BeforeEach
    void setUp() {
        engine = new BonusCalculatorEngine();
    }

    @Test
    @DisplayName("FIXED: retorna o valor fixo configurado")
    void calculateFixedBonus() {
        SalesGoal goal = new SalesGoal();
        goal.setBonusType(BonusType.FIXED);
        goal.setBonusValue(new BigDecimal("5000.00"));

        BigDecimal bonus = engine.calculateBonus(goal, new BigDecimal("100000.00"), new BigDecimal("30000.00"));
        assertThat(bonus).isEqualByComparingTo("5000.00");
    }

    @Test
    @DisplayName("PERCENTAGE_OF_REVENUE: calcula percentagem sobre receita")
    void calculateRevenuePercentageBonus() {
        SalesGoal goal = new SalesGoal();
        goal.setBonusType(BonusType.PERCENTAGE_OF_REVENUE);
        goal.setBonusValue(new BigDecimal("2.50")); // 2.5%

        BigDecimal bonus = engine.calculateBonus(goal, new BigDecimal("200000.00"), new BigDecimal("50000.00"));
        assertThat(bonus).isEqualByComparingTo("5000.00"); // 200.000 * 2.5% = 5.000
    }

    @Test
    @DisplayName("PERCENTAGE_OF_MARGIN: calcula percentagem sobre margem bruta")
    void calculateMarginPercentageBonus() {
        SalesGoal goal = new SalesGoal();
        goal.setBonusType(BonusType.PERCENTAGE_OF_MARGIN);
        goal.setBonusValue(new BigDecimal("10.00")); // 10%

        BigDecimal bonus = engine.calculateBonus(goal, new BigDecimal("200000.00"), new BigDecimal("60000.00"));
        assertThat(bonus).isEqualByComparingTo("6000.00"); // 60.000 * 10% = 6.000
    }

    @Test
    @DisplayName("CDC-BIZ-05: Aplica tecto máximo bonusCap quando o valor calculado ultrapassa o limite")
    void applyBonusCap() {
        SalesGoal goal = new SalesGoal();
        goal.setBonusType(BonusType.PERCENTAGE_OF_REVENUE);
        goal.setBonusValue(new BigDecimal("10.00")); // 10% de 100.000 = 10.000
        goal.setBonusCap(new BigDecimal("3000.00")); // Tecto máximo 3.000

        BigDecimal bonus = engine.calculateBonus(goal, new BigDecimal("100000.00"), BigDecimal.ZERO);
        assertThat(bonus).isEqualByComparingTo("3000.00"); // Limitado pelo cap
    }

    @Test
    @DisplayName("Valores nulos retornam zero em segurança")
    void handleNullsSafely() {
        assertThat(engine.calculateBonus(null, null, null)).isEqualByComparingTo(BigDecimal.ZERO);

        SalesGoal goal = new SalesGoal();
        assertThat(engine.calculateBonus(goal, null, null)).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
