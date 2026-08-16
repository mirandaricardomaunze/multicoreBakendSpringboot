package mz.multicore.erp.modules.comercial.model;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import java.util.Map;
import java.util.Set;

public enum OrderFulfillmentStatus {
    AWAITING_SEPARATION, IN_SEPARATION, SEPARATED, INVOICED, CANCELLED;

    private static final Map<OrderFulfillmentStatus, Set<OrderFulfillmentStatus>> TRANSITIONS = Map.of(
            AWAITING_SEPARATION, Set.of(IN_SEPARATION, CANCELLED),
            IN_SEPARATION, Set.of(SEPARATED, CANCELLED),
            SEPARATED, Set.of(INVOICED, CANCELLED),
            INVOICED, Set.of(), CANCELLED, Set.of());

    public void requireTransitionTo(OrderFulfillmentStatus target) {
        if (!TRANSITIONS.get(this).contains(target)) {
            throw new BusinessRuleException("Transicao de pedido invalida: " + this + " para " + target + ".");
        }
    }

    public static boolean isFulfillmentStatus(String value) {
        if (value == null) return false;
        try { valueOf(value); return true; } catch (IllegalArgumentException ignored) { return false; }
    }
}
