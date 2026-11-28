package mz.multicore.erp.modules.onlineshop.dto;

import jakarta.validation.constraints.Size;

/**
 * The customer following an online order (docs/LOJA_ONLINE_SPEC.md §3.3, LO-09): the order number and the mobile phone
 * given at checkout. Sent in the body, never in the URL, so the phone stays out of access logs. Missing values are not
 * rejected here: they answer "Encomenda não encontrada." like any other miss, so nothing tells a miss apart.
 */
public record LookupOnlineShopOrderRequest(@Size(max = 60, message = "Número de encomenda inválido.") String number,
                                           @Size(max = 30, message = "Telemóvel inválido.") String phone) {
}
