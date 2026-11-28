package mz.multicore.erp.modules.onlineshop.dto;

/** Whether a product shows on the online shop: "Publicar na loja online" (docs/LOJA_ONLINE_SPEC.md §3.5, LO-UI-02). */
public record OnlineShopPublicationDTO(Long productId, boolean published) {
}
