package mz.multicore.erp.modules.onlineshop.dto;

import java.math.BigDecimal;

/**
 * Product shown in the public online shop (docs/LOJA_ONLINE_SPEC.md §3.1–3.2, LO-02/LO-03): only sale data taken
 * from the product register and whether it is Disponível ({@code available}) or Esgotado; never costs, suppliers or
 * quantities.
 */
public record OnlineShopProductDTO(String sku,
                                   String name,
                                   String category,
                                   BigDecimal priceWithVat,
                                   boolean available) {
}
