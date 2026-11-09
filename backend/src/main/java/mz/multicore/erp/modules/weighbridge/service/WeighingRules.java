package mz.multicore.erp.modules.weighbridge.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.modules.weighbridge.model.WeighingDirection;

import java.math.BigDecimal;

/**
 * Pure weighing rules. A truck that brings material (IN) must leave lighter; one that takes material (OUT) must
 * leave heavier. The net weight is the difference, never negative, so a swapped weighing is refused instead of
 * being recorded with the wrong sign.
 */
public final class WeighingRules {

    private WeighingRules() {}

    public static BigDecimal net(WeighingDirection direction, BigDecimal first, BigDecimal second) {
        BigDecimal net = direction == WeighingDirection.IN ? first.subtract(second) : second.subtract(first);
        if (net.signum() <= 0) {
            String expected = direction == WeighingDirection.IN ? "sair mais leve do que entrou" : "sair mais pesado do que entrou";
            throw new BusinessRuleException("O camião devia " + expected + ": 1.ª pesagem " + DisplayNumbers.qty(first)
                    + " kg, 2.ª pesagem " + DisplayNumbers.qty(second) + " kg.");
        }
        return net;
    }

    /** Plates are compared without spaces or dashes and in upper case ("ABC-123 MC" = "abc123mc"). */
    public static String normalizePlate(String plate) {
        return plate == null ? "" : plate.toUpperCase(java.util.Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }
}
