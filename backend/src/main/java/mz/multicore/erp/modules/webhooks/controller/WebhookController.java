package mz.multicore.erp.modules.webhooks.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.webhooks.dto.CreateWebhookSubscriptionRequest;
import mz.multicore.erp.modules.webhooks.dto.CreatedWebhookSubscriptionDTO;
import mz.multicore.erp.modules.webhooks.dto.WebhookAlertDTO;
import mz.multicore.erp.modules.webhooks.dto.WebhookDeliveryDTO;
import mz.multicore.erp.modules.webhooks.dto.WebhookSubscriptionDTO;
import mz.multicore.erp.modules.webhooks.service.WebhookDeliveryService;
import mz.multicore.erp.modules.webhooks.service.WebhookSubscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Internal management of the active company's webhooks (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.3.5, WH-08): user login
 * only, ADMIN only ({@code ApiRolePolicy} and the Service guards). The secret leaves only in the 201 of the creation.
 */
@RestController
@RequestMapping("/api/integrations/webhooks")
public class WebhookController {

    private final WebhookSubscriptionService subscriptionService;
    private final WebhookDeliveryService deliveryService;

    public WebhookController(WebhookSubscriptionService subscriptionService, WebhookDeliveryService deliveryService) {
        this.subscriptionService = subscriptionService;
        this.deliveryService = deliveryService;
    }

    @GetMapping
    public List<WebhookSubscriptionDTO> list() {
        return subscriptionService.list();
    }

    @PostMapping
    public ResponseEntity<CreatedWebhookSubscriptionDTO> create(
            @Valid @RequestBody CreateWebhookSubscriptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subscriptionService.create(request));
    }

    @PostMapping("/{id}/activate")
    public WebhookSubscriptionDTO activate(@PathVariable Long id) {
        return subscriptionService.activate(id);
    }

    @PostMapping("/{id}/deactivate")
    public WebhookSubscriptionDTO deactivate(@PathVariable Long id) {
        return subscriptionService.deactivate(id);
    }

    @GetMapping("/alerts")
    public List<WebhookAlertDTO> alerts() {
        return subscriptionService.autoDisabledAlerts();
    }

    @GetMapping("/{id}/deliveries")
    public List<WebhookDeliveryDTO> deliveries(@PathVariable Long id,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(required = false) Integer size) {
        return deliveryService.listDeliveries(id, status, page, size);
    }

    @PostMapping("/deliveries/{deliveryId}/resend")
    public WebhookDeliveryDTO resend(@PathVariable Long deliveryId) {
        return deliveryService.resend(deliveryId);
    }
}
