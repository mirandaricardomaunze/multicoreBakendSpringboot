package mz.multicore.erp.architecture.events;

import java.time.OffsetDateTime;

/**
 * Uma encomenda de cliente mudou de estado (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.2.1, WH-02). Só se publica quando
 * o estado muda de facto; a criação da encomenda não é mudança de estado. Leva só números e textos, nunca entidades,
 * para que os webhooks não fiquem presos ao modelo do módulo comercial (mesmo padrão do {@link SaleRegisteredEvent}).
 *
 * @param previousStatus nome do {@code OrderStatus} anterior (ex.: {@code PENDING})
 * @param status         nome do {@code OrderStatus} novo (ex.: {@code BILLED})
 * @param occurredAt     instante da mudança
 */
public record OrderStatusChangedEvent(
        Long companyId,
        Long orderId,
        String orderNumber,
        String previousStatus,
        String status,
        OffsetDateTime occurredAt
) {
    /** {@code true} quando os dois estados são iguais — nesse caso não há evento a avisar. */
    public boolean unchanged() {
        return previousStatus != null && previousStatus.equals(status);
    }
}
