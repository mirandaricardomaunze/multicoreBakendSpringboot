package com.phcpro.architecture.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure utility for line-level money math used across Comercial, POS and Purchase modules.
 * Centralises the rule:
 *     subtotal  = unitPrice * quantity
 *     discount  = subtotal * (discountPct / 100)
 *     net       = subtotal - discount
 *     tax       = net * taxRate
 *     total     = net + tax
 * Final monetary values are rounded HALF_UP to 2 decimal places.
 */
public final class LineCalculator {

    private static final int MONEY_SCALE = 2;
    private static final int RATIO_SCALE = 4;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private LineCalculator() {}

    public static LineAmounts compute(BigDecimal unitPrice, BigDecimal quantity, BigDecimal discountPercentage, BigDecimal taxRate) {
        BigDecimal qty = quantity == null ? BigDecimal.ZERO : quantity;
        BigDecimal price = unitPrice == null ? BigDecimal.ZERO : unitPrice;
        BigDecimal discountPct = discountPercentage == null ? BigDecimal.ZERO : discountPercentage;
        BigDecimal rate = taxRate == null ? BigDecimal.ZERO : taxRate;

        BigDecimal subtotal = price.multiply(qty);
        BigDecimal discount = BigDecimal.ZERO;
        if (discountPct.compareTo(BigDecimal.ZERO) > 0) {
            discount = subtotal.multiply(discountPct.divide(HUNDRED, RATIO_SCALE, RoundingMode.HALF_UP));
        }
        BigDecimal net = subtotal.subtract(discount);
        BigDecimal tax = net.multiply(rate);
        BigDecimal total = net.add(tax);

        return new LineAmounts(
                net,
                tax,
                total.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
        );
    }

    public static LineAmounts compute(BigDecimal unitPrice, int quantity, BigDecimal discountPercentage, BigDecimal taxRate) {
        return compute(unitPrice, BigDecimal.valueOf(quantity), discountPercentage, taxRate);
    }

    public record LineAmounts(BigDecimal net, BigDecimal tax, BigDecimal total) {}
}
