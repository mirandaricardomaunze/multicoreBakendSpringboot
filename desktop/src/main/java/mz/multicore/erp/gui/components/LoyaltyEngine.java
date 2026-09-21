package mz.multicore.erp.gui.components;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Motor canónico do programa de fidelização de clientes e resgate de pontos no POS.
 */
public class LoyaltyEngine {

    /** 1 ponto gerado por cada 100 MT em compras. */
    public static final BigDecimal SPEND_PER_POINT = new BigDecimal("100.00");

    /** Cada ponto equivale a 1.00 MT de desconto na compra. */
    public static final BigDecimal POINT_VALUE_MZN = new BigDecimal("1.00");

    public record RedemptionResult(
            BigDecimal pointsRedeemed,
            BigDecimal discountMzn,
            BigDecimal finalPayableAmount,
            BigDecimal remainingPoints
    ) {}

    /** Calcula os pontos ganhos numa venda com base no total pago. */
    public static BigDecimal calculateEarnedPoints(BigDecimal totalAmount) {
        if (totalAmount == null || totalAmount.compareTo(SPEND_PER_POINT) < 0) {
            return BigDecimal.ZERO.setScale(0, RoundingMode.DOWN);
        }
        return totalAmount.divideToIntegralValue(SPEND_PER_POINT).setScale(0, RoundingMode.DOWN);
    }

    /** Converte pontos em valor equivalente em Meticais. */
    public static BigDecimal pointsToValueMzn(BigDecimal points) {
        if (points == null || points.signum() <= 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return points.multiply(POINT_VALUE_MZN).setScale(2, RoundingMode.HALF_UP);
    }

    /** Calcula o limite máximo de pontos que podem ser abatidos nesta venda. */
    public static BigDecimal calculateMaxRedeemablePoints(BigDecimal availablePoints, BigDecimal saleTotal) {
        if (availablePoints == null || saleTotal == null || availablePoints.signum() <= 0 || saleTotal.signum() <= 0) {
            return BigDecimal.ZERO.setScale(0, RoundingMode.DOWN);
        }
        BigDecimal maxPointsBySale = saleTotal.divideToIntegralValue(POINT_VALUE_MZN);
        return availablePoints.min(maxPointsBySale).setScale(0, RoundingMode.DOWN);
    }

    /** Aplica o resgate de pontos, calculando o desconto, o novo total a pagar e o saldo remanescente. */
    public static RedemptionResult applyRedemption(BigDecimal availablePoints, BigDecimal saleTotal, BigDecimal pointsToRedeem) {
        if (availablePoints == null) availablePoints = BigDecimal.ZERO;
        if (saleTotal == null) saleTotal = BigDecimal.ZERO;
        if (pointsToRedeem == null || pointsToRedeem.signum() <= 0) {
            return new RedemptionResult(BigDecimal.ZERO, BigDecimal.ZERO, saleTotal, availablePoints);
        }

        BigDecimal maxPoints = calculateMaxRedeemablePoints(availablePoints, saleTotal);
        BigDecimal actualPoints = pointsToRedeem.min(maxPoints).setScale(0, RoundingMode.DOWN);
        BigDecimal discount = pointsToValueMzn(actualPoints);
        BigDecimal finalAmount = saleTotal.subtract(discount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal remaining = availablePoints.subtract(actualPoints).max(BigDecimal.ZERO);

        return new RedemptionResult(actualPoints, discount, finalAmount, remaining);
    }
}
