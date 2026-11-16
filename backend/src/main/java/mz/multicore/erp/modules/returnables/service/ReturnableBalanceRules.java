package mz.multicore.erp.modules.returnables.service;

import mz.multicore.erp.modules.returnables.model.ReturnableMovementType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Pure returnables account rules: balance and how long the oldest unreturned unit has been out (FIFO). */
public final class ReturnableBalanceRules {

    public record Entry(LocalDate date, ReturnableMovementType type, BigDecimal quantity) {}

    private ReturnableBalanceRules() {}

    public static BigDecimal balance(List<Entry> entries) {
        BigDecimal balance = BigDecimal.ZERO;
        for (Entry e : entries) balance = e.type() == ReturnableMovementType.OUT ? balance.add(e.quantity()) : balance.subtract(e.quantity());
        return balance;
    }

    /** Date of the oldest delivery not yet covered by returns or charges, or {@code null} when nothing is out. */
    public static LocalDate oldestOpen(List<Entry> chronological) {
        BigDecimal settled = chronological.stream().filter(e -> e.type() != ReturnableMovementType.OUT)
                .map(Entry::quantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        for (Entry e : chronological) {
            if (e.type() != ReturnableMovementType.OUT) continue;
            if (settled.compareTo(e.quantity()) >= 0) {
                settled = settled.subtract(e.quantity());
            } else {
                return e.date();
            }
        }
        return null;
    }
}
