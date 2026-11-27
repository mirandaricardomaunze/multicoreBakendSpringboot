package mz.multicore.erp.modules.publicapi.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.publicapi.dto.ApiKeyPrincipal;
import mz.multicore.erp.modules.publicapi.dto.CreatePublicOrderRequest;
import mz.multicore.erp.modules.publicapi.dto.PublicOrderDTO;
import mz.multicore.erp.modules.publicapi.service.PublicOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public API orders (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.4, API-08). Authentication, {@code ORDERS_WRITE} and the
 * rate limit are applied before this runs.
 */
@RestController
@RequestMapping("/api/public/v1/orders")
public class PublicOrderController {

    private final PublicOrderService orderService;

    public PublicOrderController(PublicOrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<PublicOrderDTO> create(@AuthenticationPrincipal ApiKeyPrincipal key,
                                                 @RequestBody @Valid CreatePublicOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(key, request));
    }
}
