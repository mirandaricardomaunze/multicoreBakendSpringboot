package mz.multicore.erp.modules.pos.service;

import mz.multicore.erp.modules.pos.dto.PosPaymentRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Predicate;

/**
 * How a POS sale was paid.
 *
 * @param declared every payment line, to refuse a sum above the sale total
 * @param settled  what settles the invoice now: everything except CREDIT (sold on account, still owed)
 * @param money    money that came in: settled minus STORE_CREDIT (a voucher offsets the customer's credit, not cash)
 * @param cash     the part of {@code money} paid in cash; the rest went to bank accounts (card, transfer, M-Pesa)
 */
record PosPaymentSplit(BigDecimal declared, BigDecimal settled, BigDecimal money, BigDecimal cash) {

    static PosPaymentSplit of(List<PosPaymentRequest> payments) {
        return new PosPaymentSplit(sum(payments, p -> true), sum(payments, p -> !is(p, "CREDIT")),
                sum(payments, p -> !is(p, "CREDIT") && !is(p, "STORE_CREDIT")), sum(payments, p -> is(p, "CASH")));
    }

    private static boolean is(PosPaymentRequest payment, String method) {
        return method.equalsIgnoreCase(payment.method());
    }

    private static BigDecimal sum(List<PosPaymentRequest> payments, Predicate<PosPaymentRequest> filter) {
        return payments.stream().filter(filter).map(PosPaymentRequest::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }
}
