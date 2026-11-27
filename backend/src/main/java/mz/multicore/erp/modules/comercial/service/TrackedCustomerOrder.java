package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.dto.OrderDTO;

/**
 * Encomenda de cliente tal como um sistema externo a acompanha (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.4.3).
 *
 * @param order             a encomenda no estado actual
 * @param externalReference referência do sistema que a criou; {@code null} nas do desktop
 * @param invoiceNumber     número da factura associada; {@code null} enquanto não estiver facturada
 */
public record TrackedCustomerOrder(OrderDTO order, String externalReference, String invoiceNumber) {
}
