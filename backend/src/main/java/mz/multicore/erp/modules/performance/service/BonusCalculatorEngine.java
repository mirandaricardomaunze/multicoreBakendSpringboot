package mz.multicore.erp.modules.performance.service;

import mz.multicore.erp.modules.performance.model.BonusType;
import mz.multicore.erp.modules.performance.model.SalesGoal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class BonusCalculatorEngine {

    public BigDecimal calculateBonus(SalesGoal goal, BigDecimal currentRevenue, BigDecimal currentMargin) {
        if (goal == null || goal.getBonusType() == null || goal.getBonusValue() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal revenue = currentRevenue != null ? currentRevenue : BigDecimal.ZERO;
        BigDecimal margin = currentMargin != null ? currentMargin : BigDecimal.ZERO;
        BigDecimal bonusValue = goal.getBonusValue();

        BigDecimal calculated;
        if (goal.getBonusType() == BonusType.FIXED) {
            calculated = bonusValue;
        } else if (goal.getBonusType() == BonusType.PERCENTAGE_OF_REVENUE) {
            calculated = revenue.multiply(bonusValue).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else if (goal.getBonusType() == BonusType.PERCENTAGE_OF_MARGIN) {
            calculated = margin.multiply(bonusValue).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            calculated = BigDecimal.ZERO;
        }

        // CDC-BIZ-05: Aplicar bonusCap se definido (min(calculado, bonusCap))
        if (goal.getBonusCap() != null && goal.getBonusCap().compareTo(BigDecimal.ZERO) > 0) {
            calculated = calculated.min(goal.getBonusCap());
        }

        return calculated.max(BigDecimal.ZERO);
    }
}
