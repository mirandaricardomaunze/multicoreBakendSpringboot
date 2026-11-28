package mz.multicore.erp.modules.onlineshop.controller;

import mz.multicore.erp.modules.onlineshop.dto.OnlineShopPublicationDTO;
import mz.multicore.erp.modules.onlineshop.dto.SetOnlineShopPublicationRequest;
import mz.multicore.erp.modules.onlineshop.service.OnlineShopCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal endpoints behind the "Publicar na loja online" box of the desktop product form
 * (docs/LOJA_ONLINE_SPEC.md §3.5, LO-UI-02): whoever reads products reads the state, whoever edits products changes
 * it ({@code ApiRolePolicy}). The service audits the change and keeps the LO-02 refusals.
 */
@RestController
@RequestMapping("/api/online-shop/products/{productId}/published")
public class OnlineShopProductController {

    private final OnlineShopCatalogService catalogService;

    public OnlineShopProductController(OnlineShopCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public OnlineShopPublicationDTO get(@PathVariable Long productId) {
        return catalogService.publication(productId);
    }

    @PutMapping
    public OnlineShopPublicationDTO set(@PathVariable Long productId,
                                        @Valid @RequestBody SetOnlineShopPublicationRequest request) {
        return catalogService.setPublished(productId, request);
    }
}
