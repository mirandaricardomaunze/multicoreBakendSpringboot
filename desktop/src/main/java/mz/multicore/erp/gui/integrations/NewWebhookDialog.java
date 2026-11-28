package mz.multicore.erp.gui.integrations;

import mz.multicore.erp.desktop.client.WebhookApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.webhooks.dto.CreateWebhookSubscriptionRequest;
import mz.multicore.erp.modules.webhooks.dto.CreatedWebhookSubscriptionDTO;
import mz.multicore.erp.modules.webhooks.model.WebhookEventType;

import javax.swing.*;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * "Novo webhook" (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.4.2): URL and one box per event, checked locally with the
 * backend's messages; the backend decides (private address, http in production, …) and its refusal is shown in the
 * dialog as it comes. The events travel as their wire codes.
 */
final class NewWebhookDialog {

    private final WebhookApiClient webhookApiClient;

    NewWebhookDialog(WebhookApiClient webhookApiClient) {
        this.webhookApiClient = webhookApiClient;
    }

    /** Opens the dialog; the created subscription with its secret, or {@code null} when cancelled. */
    CreatedWebhookSubscriptionDTO show() {
        JTextField url = new JTextField();
        UIHelper.styleTextField(url);
        Map<WebhookEventType, JCheckBox> boxes = new EnumMap<>(WebhookEventType.class);
        JPanel events = new JPanel();
        events.setOpaque(false);
        events.setLayout(new BoxLayout(events, BoxLayout.Y_AXIS));
        for (WebhookEventType type : WebhookEventType.values()) {
            JCheckBox box = new JCheckBox(WebhookLabels.event(type));
            box.setOpaque(false);
            box.setForeground(UIHelper.TEXT_LIGHT);
            boxes.put(type, box);
            events.add(box);
        }

        JPanel form = UIHelper.createDialogForm(
                "", new FormField("URL", url, true, "Ex.: https://loja.exemplo.co.mz/webhooks/multicore"),
                "Eventos:", events);
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Novo webhook", "fas-satellite-dish",
                "Aviso assinado da empresa activa para um sistema externo", form);
        AtomicReference<CreatedWebhookSubscriptionDTO> created = new AtomicReference<>();
        dialog.setOnSaveAsync(() -> {
            Set<WebhookEventType> chosen = EnumSet.noneOf(WebhookEventType.class);
            boxes.forEach((type, box) -> {
                if (box.isSelected()) chosen.add(type);
            });
            CreateWebhookSubscriptionRequest request = validated(url.getText(), chosen);
            return () -> {
                created.set(webhookApiClient.create(request));
                return created.get();
            };
        });
        return dialog.showDialog() ? created.get() : null;
    }

    /** Same messages as the backend (§4.1.1); throws when the request would surely be refused. */
    static CreateWebhookSubscriptionRequest validated(String rawUrl, Set<WebhookEventType> events) {
        String url = rawUrl == null ? "" : rawUrl.trim();
        if (url.isEmpty()) {
            throw new IllegalArgumentException("Indique o URL do webhook.");
        }
        if (events == null || events.isEmpty()) {
            throw new IllegalArgumentException("Escolha pelo menos um evento.");
        }
        return new CreateWebhookSubscriptionRequest(url, Arrays.stream(WebhookEventType.values())
                .filter(events::contains).map(WebhookEventType::code).toList());
    }
}
