package mz.multicore.erp.modules.publicapi.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.publicapi.dto.ApiKeyPrincipal;
import mz.multicore.erp.modules.publicapi.dto.CreatePublicOrderRequest;
import mz.multicore.erp.modules.publicapi.dto.PublicOrderDTO;
import mz.multicore.erp.modules.publicapi.dto.PublicOrderSubmission;
import mz.multicore.erp.modules.publicapi.service.PublicOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public API orders (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.4, API-08, API-09): {@code 201} when created, {@code 200}
 * when the same reference and content is replayed; reading by number (§3.4.3, API-10) with {@code ORDERS_READ}.
 * Authentication, the route's permission and the rate limit are applied before this runs.
 */
@RestController
@RequestMapping("/api/public/v1/orders")
public class PublicOrderController {

    private final PublicOrderService orderService;

    public PublicOrderController(PublicOrderService orderService) {
        this.orderService = orderService;
    }

    /** The number has a slash ({@code ENC-2026/00001}) and goes as is in the path: {@code {*number}} takes all of it. */
    @GetMapping("/{*number}")
    public PublicOrderDTO get(@AuthenticationPrincipal ApiKeyPrincipal key, @PathVariable String number) {
        return orderService.getOrder(key, number.startsWith("/") ? number.substring(1) : number);
    }

    @PostMapping
    public ResponseEntity<PublicOrderDTO> create(@AuthenticationPrincipal ApiKeyPrincipal key,
                                                 @RequestBody @Valid CreatePublicOrderRequest request) {
        PublicOrderSubmission submission = orderService.submit(key, request);
        return ResponseEntity.status(submission.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(submission.order());
    }
}
