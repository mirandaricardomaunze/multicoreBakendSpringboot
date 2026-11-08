package mz.multicore.erp.modules.pickup.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.format.DisplayNumbers;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pure balance rules for paid material awaiting pickup (SA-C07), free of I/O. */
public final class PickupBalanceRules {

    /** Requested handover of one invoice line. */
    public record Request(Long invoiceLineId, BigDecimal quantity) {}

    private PickupBalanceRules() {
    }

    /** What remains to collect; never negative even if a credit note covered more than was left. */
    public static BigDecimal balance(BigDecimal invoiced, BigDecimal credited, BigDecimal picked) {
        BigDecimal remaining = nz(invoiced).subtract(nz(credited)).subtract(nz(picked));
        return remaining.signum() < 0 ? BigDecimal.ZERO : remaining;
    }

    /**
     * Validates a handover against the current balances (keyed by invoice line id). Each line must exist,
     * appear once, be positive and fit in its balance.
     */
    public static void validate(List<Request> requests, Map<Long, BigDecimal> balances, Map<Long, String> names) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessRuleException("Indique pelo menos uma linha a levantar.");
        }
        Set<Long> seen = new HashSet<>();
        boolean any = false;
        for (Request request : requests) {
            if (request.invoiceLineId() == null || !balances.containsKey(request.invoiceLineId())) {
                throw new BusinessRuleException("A linha indicada não pertence a esta fatura.");
            }
            if (!seen.add(request.invoiceLineId())) {
                throw new BusinessRuleException("A mesma linha aparece duas vezes no levantamento.");
            }
            BigDecimal quantity = nz(request.quantity());
            if (quantity.signum() < 0) {
                throw new BusinessRuleException("A quantidade a levantar não pode ser negativa.");
            }
            if (quantity.signum() == 0) continue;
            any = true;
            BigDecimal balance = balances.get(request.invoiceLineId());
            if (quantity.compareTo(balance) > 0) {
                String name = names.getOrDefault(request.invoiceLineId(), "artigo");
                throw new BusinessRuleException("Só restam " + DisplayNumbers.qty(balance) + " de " + name
                        + " por levantar; pediu " + DisplayNumbers.qty(quantity) + ".");
            }
        }
        if (!any) throw new BusinessRuleException("Indique uma quantidade maior que zero em pelo menos uma linha.");
    }

    public static boolean allCollected(Iterable<BigDecimal> balances) {
        for (BigDecimal balance : balances) {
            if (nz(balance).signum() > 0) return false;
        }
        return true;
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
