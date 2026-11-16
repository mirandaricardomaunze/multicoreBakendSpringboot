package mz.multicore.erp.modules.accounting.model;

import java.math.BigDecimal;

/**
 * Nature of an account balance, indicating the side on which it increases.
 *
 * <p>Determines whether a trial-balance amount is <b>debit</b> or <b>credit</b>. For debit-nature
 * accounts (Cash, Customers, Expenses), the balance is {@code debit − credit}; credit-nature
 * accounts (Suppliers, Equity, Revenue) use the reverse. This prevents inverted balance signs.
 */
public enum AccountNature {

    /** Increases on the debit side: assets and expenses. */
    DEVEDORA,

    /** Increases on the credit side: liabilities, equity, and revenue. */
    CREDORA;

    /** Account balance with the sign appropriate to its nature. */
    public BigDecimal balanceOf(BigDecimal totalDebit, BigDecimal totalCredit) {
        BigDecimal debit = totalDebit == null ? BigDecimal.ZERO : totalDebit;
        BigDecimal credit = totalCredit == null ? BigDecimal.ZERO : totalCredit;
        return this == DEVEDORA ? debit.subtract(credit) : credit.subtract(debit);
    }
}
