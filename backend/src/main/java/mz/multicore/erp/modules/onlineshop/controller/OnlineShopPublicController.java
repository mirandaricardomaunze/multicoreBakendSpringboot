package mz.multicore.erp.modules.onlineshop.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.onlineshop.dto.LookupOnlineShopOrderRequest;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopOrderDTO;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopProductDTO;
import mz.multicore.erp.modules.onlineshop.dto.PlaceOnlineShopOrderRequest;
import mz.multicore.erp.modules.onlineshop.service.OnlineShopCatalogService;
import mz.multicore.erp.modules.onlineshop.service.OnlineShopOrderService;
import mz.multicore.erp.modules.onlineshop.service.OnlineShopOrderTrackingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public endpoints of the online shop behind {@code /loja/{code}} (docs/LOJA_ONLINE_SPEC.md §3.2, LO-04): no login,
 * read-only catalogue, order placement and the customer's order lookup by number + phone (§3.3, LO-09). The per-IP
 * limits run before this ({@code OnlineShopRateLimitFilter}); a disabled or unknown shop answers "Loja indisponível.".
 */
@RestController
@RequestMapping("/api/shop/{code}")
public class OnlineShopPublicController {

    private final OnlineShopCatalogService catalogService;
    private final OnlineShopOrderService orderService;
    private final OnlineShopOrderTrackingService trackingService;

    public OnlineShopPublicController(OnlineShopCatalogService catalogService, OnlineShopOrderService orderService,
                                      OnlineShopOrderTrackingService trackingService) {
        this.catalogService = catalogService;
        this.orderService = orderService;
        this.trackingService = trackingService;
    }

    @GetMapping("/products")
    public List<OnlineShopProductDTO> products(@PathVariable String code) {
        return catalogService.listProducts(code);
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OnlineShopOrderDTO placeOrder(@PathVariable String code,
                                         @Valid @RequestBody PlaceOnlineShopOrderRequest request) {
        return orderService.placeOrder(code, request);
    }

    /** The phone goes in the body, not the URL, so it stays out of access logs; not held to the hourly order limit. */
    @PostMapping("/orders/lookup")
    public OnlineShopOrderDTO lookupOrder(@PathVariable String code,
                                          @Valid @RequestBody LookupOnlineShopOrderRequest request) {
        return trackingService.findOrder(code, request);
    }
}
