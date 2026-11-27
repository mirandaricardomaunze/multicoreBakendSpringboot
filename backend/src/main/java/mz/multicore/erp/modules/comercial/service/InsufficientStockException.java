package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;

/**
 * Stock livre (físico − reservas activas) não chega para a saída ou a reserva pedida. Subclasse própria para a
 * API pública responder com um {@code code} estável (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.4.1).
 */
public class InsufficientStockException extends BusinessRuleException {

    public InsufficientStockException(String message) {
        super(message);
    }
}
