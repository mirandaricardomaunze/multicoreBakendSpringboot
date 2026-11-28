package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.webhooks.dto.CreateWebhookSubscriptionRequest;
import mz.multicore.erp.modules.webhooks.dto.CreatedWebhookSubscriptionDTO;
import mz.multicore.erp.modules.webhooks.dto.WebhookAlertDTO;
import mz.multicore.erp.modules.webhooks.dto.WebhookDeliveryDTO;
import mz.multicore.erp.modules.webhooks.dto.WebhookSubscriptionDTO;
import mz.multicore.erp.modules.webhooks.model.WebhookDeliveryStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * HTTP client for the webhooks of the active company ({@code /api/integrations/webhooks},
 * docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.3.5). The desktop never generates, stores nor logs a secret: it only shows
 * the one returned by {@link #create}.
 */
@Component
@Profile("desktop")
public class WebhookApiClient {

    private static final String BASE = "/api/integrations/webhooks";
    /** The delivery log shows the 50 most recent deliveries (§4.4.2). */
    static final int DELIVERY_PAGE_SIZE = 50;

    private final DesktopClientFactory clientFactory;

    public WebhookApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<WebhookSubscriptionDTO> list() {
        return clientFactory.authenticatedClient().getList(BASE, WebhookSubscriptionDTO.class);
    }

    public CreatedWebhookSubscriptionDTO create(CreateWebhookSubscriptionRequest request) {
        return clientFactory.authenticatedClient().post(BASE, request, CreatedWebhookSubscriptionDTO.class);
    }

    public WebhookSubscriptionDTO activate(Long id) {
        return clientFactory.authenticatedClient().post(BASE + "/" + id + "/activate", null, WebhookSubscriptionDTO.class);
    }

    public WebhookSubscriptionDTO deactivate(Long id) {
        return clientFactory.authenticatedClient().post(BASE + "/" + id + "/deactivate", null, WebhookSubscriptionDTO.class);
    }

    public List<WebhookAlertDTO> alerts() {
        return clientFactory.authenticatedClient().getList(BASE + "/alerts", WebhookAlertDTO.class);
    }

    /** The 50 most recent deliveries of a subscription; {@code status == null} means all states. */
    public List<WebhookDeliveryDTO> deliveries(Long subscriptionId, WebhookDeliveryStatus status) {
        return clientFactory.authenticatedClient().getList(deliveriesPath(subscriptionId, status), WebhookDeliveryDTO.class);
    }

    public WebhookDeliveryDTO resend(Long deliveryId) {
        return clientFactory.authenticatedClient().post(BASE + "/deliveries/" + deliveryId + "/resend", null,
                WebhookDeliveryDTO.class);
    }

    public static String deliveriesPath(Long subscriptionId, WebhookDeliveryStatus status) {
        String path = BASE + "/" + subscriptionId + "/deliveries?page=0&size=" + DELIVERY_PAGE_SIZE;
        return status == null ? path : path + "&status=" + status.name();
    }
}
