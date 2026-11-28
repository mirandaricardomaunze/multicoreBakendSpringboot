package mz.multicore.erp.modules.onlineshop.dto;

/** Pickup store offered at the online checkout (docs/LOJA_ONLINE_SPEC.md §3.3): the id sent back and its name. */
public record OnlineShopPickupStoreDTO(Long id, String name) {
}
