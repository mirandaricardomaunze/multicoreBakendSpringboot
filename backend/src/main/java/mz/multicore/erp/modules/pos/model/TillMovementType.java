package mz.multicore.erp.modules.pos.model;

import java.math.BigDecimal;

/**
 * Tipo de movimento de caixa (gaveta).
 * SALE/SUPRIMENTO = entradas; SANGRIA/REFUND = saidas.
 */
public enum TillMovementType {
    SALE(1),
    SUPRIMENTO(1),
    SANGRIA(-1),
    REFUND(-1),
    /**
     * Cash received at the counter for an invoice receipt (sinal, vasilhame, debt). It is in the drawer, but the
     * receipt already debited its treasury account, so the close deposit leaves it out.
     */
    COUNTER_RECEIPT(1),
    /** A counter receipt cancelled while the session is open: the cash leaves the drawer again. */
    COUNTER_RECEIPT_REVERSAL(-1);

    private final int cashSign;

    TillMovementType(int cashSign) {
        this.cashSign = cashSign;
    }

    /** The amount as it changes the cash in the drawer: positive for entries, negative for exits. */
    public BigDecimal signed(BigDecimal amount) {
        return cashSign > 0 ? amount : amount.negate();
    }

    /** Counter receipts are created from the receipt itself, never by hand at the till. */
    public boolean counterReceipt() {
        return this == COUNTER_RECEIPT || this == COUNTER_RECEIPT_REVERSAL;
    }
}
