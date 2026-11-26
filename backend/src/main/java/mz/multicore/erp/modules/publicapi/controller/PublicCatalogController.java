package mz.multicore.erp.modules.publicapi.controller;

import mz.multicore.erp.modules.publicapi.dto.ApiKeyPrincipal;
import mz.multicore.erp.modules.publicapi.dto.PublicPage;
import mz.multicore.erp.modules.publicapi.dto.PublicProductDTO;
import mz.multicore.erp.modules.publicapi.dto.PublicStockDTO;
import mz.multicore.erp.modules.publicapi.service.PublicCatalogService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public API catalogue (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.3, API-07): products and available stock of the calling
 * key's company. Authentication, the {@code CATALOG_READ} permission and the rate limit are applied before this runs.
 */
@RestController
@RequestMapping("/api/public/v1")
public class PublicCatalogController {

    private final PublicCatalogService catalogService;

    public PublicCatalogController(PublicCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/products")
    public PublicPage<PublicProductDTO> products(@AuthenticationPrincipal ApiKeyPrincipal key,
                                                 @RequestParam(required = false) String updatedSince,
                                                 @RequestParam(required = false) Integer page,
                                                 @RequestParam(required = false) Integer size) {
        return catalogService.listProducts(key.companyId(), updatedSince, page, size);
    }

    @GetMapping("/products/{sku}")
    public PublicProductDTO product(@AuthenticationPrincipal ApiKeyPrincipal key, @PathVariable String sku) {
        return catalogService.getProduct(key.companyId(), sku);
    }

    @GetMapping("/stock")
    public PublicPage<PublicStockDTO> stock(@AuthenticationPrincipal ApiKeyPrincipal key,
                                            @RequestParam(required = false) String warehouse,
                                            @RequestParam(required = false) Integer page,
                                            @RequestParam(required = false) Integer size) {
        return catalogService.listStock(key.companyId(), warehouse, page, size);
    }
}
