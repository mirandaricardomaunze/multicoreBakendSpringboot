package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.dto.OrderDTO;

/**
 * Encomenda já criada por um sistema externo, com a impressão digital do pedido que a criou
 * (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.4.2): quem a pede decide se a repetição é a mesma ou um conflito.
 *
 * @param order              a encomenda no estado actual
 * @param requestFingerprint SHA-256 do corpo normalizado que a criou; {@code null} nas anteriores à V158
 */
public record PlacedExternalOrder(OrderDTO order, String requestFingerprint) {
}
