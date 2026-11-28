package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.WebhookApiClient;
import mz.multicore.erp.desktop.session.SignedInUser;
import mz.multicore.erp.gui.NotificationFeed.NotificationItem;
import mz.multicore.erp.modules.webhooks.dto.WebhookAlertDTO;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Avisos dos webhooks no sino (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.4.2, WH-UI-01): cada subscrição desactivada
 * automaticamente após 20 falhas seguidas vira uma notificação de prioridade alta com o texto PT-MZ do backend que
 * abre as Configurações. Só o ADMIN os vê; outro perfil não faz pedido.
 */
final class NotificationWebhookAlertProvider {

    static final String TYPE = "Webhook";
    static final String MODULE_CARD = "config";
    static final int PRIORITY = 3;
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private NotificationWebhookAlertProvider() {}

    static void addWebhookAlerts(List<NotificationItem> items, WebhookApiClient webhookApiClient) {
        if (webhookApiClient == null || !SignedInUser.isAdmin()) return;
        try {
            List<WebhookAlertDTO> alerts = webhookApiClient.alerts();
            if (alerts == null) return;
            for (WebhookAlertDTO alert : alerts) {
                String when = alert.disabledAt() == null ? "" : WHEN.format(alert.disabledAt());
                items.add(new NotificationItem(TYPE, alert.message(), alert.url(), when, MODULE_CARD, PRIORITY));
            }
        } catch (Exception ignored) {
            // O sino nunca falha por um fornecedor: sem avisos dos webhooks, mostra os restantes.
        }
    }
}
