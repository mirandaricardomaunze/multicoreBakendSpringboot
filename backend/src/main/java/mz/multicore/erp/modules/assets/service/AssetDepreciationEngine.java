package mz.multicore.erp.modules.assets.service;

import mz.multicore.erp.modules.assets.model.Asset;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Motor de cálculo de depreciação de activos fixos segundo as regras fiscais moçambicanas (IRPC / PGC-NIRF).
 * Aplica o método das quotas constantes (depreciação linear mensal).
 */
@Component
public class AssetDepreciationEngine {

    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);

    /**
     * Calcula o valor da quota mensal de depreciação:
     * Quota Mensal = (Custo de Aquisição - Valor Residual) * (Taxa Anual / 12)
     */
    public BigDecimal calculateMonthlyDepreciation(Asset asset) {
        if (asset.getAcquisitionCost() == null || asset.getAnnualDepreciationRate() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal depreciableBase = asset.getAcquisitionCost()
                .subtract(asset.getResidualValue() != null ? asset.getResidualValue() : BigDecimal.ZERO);

        if (depreciableBase.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal monthlyRate = asset.getAnnualDepreciationRate()
                .divide(TWELVE, 8, RoundingMode.HALF_UP);

        BigDecimal theoreticalMonthly = depreciableBase.multiply(monthlyRate)
                .setScale(2, RoundingMode.HALF_UP);

        // Não pode depreciar além do valor residual
        BigDecimal remainingDepreciable = asset.getNetBookValue()
                .subtract(asset.getResidualValue() != null ? asset.getResidualValue() : BigDecimal.ZERO);

        if (remainingDepreciable.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return theoreticalMonthly.min(remainingDepreciable);
    }
}
