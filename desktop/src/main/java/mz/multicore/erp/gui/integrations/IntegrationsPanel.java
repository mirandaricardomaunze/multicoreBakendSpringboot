package mz.multicore.erp.gui.integrations;

import mz.multicore.erp.desktop.client.ApiKeyApiClient;
import mz.multicore.erp.desktop.client.OnlineShopApiClient;
import mz.multicore.erp.desktop.client.WebhookApiClient;
import mz.multicore.erp.gui.components.Spacing;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Configurações › Integrações (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.4): the public API keys (§4.4.1) and, below
 * them, the webhooks (§4.4.2) and, last, the online shop settings (docs/LOJA_ONLINE_SPEC.md §3.5, LO-UI-01).
 * {@link #refresh()} reloads every section.
 */
public class IntegrationsPanel extends JPanel {

    private final ApiKeysPanel apiKeysPanel;
    private final WebhooksPanel webhooksPanel;
    private final OnlineShopSettingsPanel onlineShopPanel;

    public IntegrationsPanel(ApiKeyApiClient apiKeyApiClient) {
        this(apiKeyApiClient, null);
    }

    public IntegrationsPanel(ApiKeyApiClient apiKeyApiClient, WebhookApiClient webhookApiClient) {
        this(apiKeyApiClient, webhookApiClient, null);
    }

    public IntegrationsPanel(ApiKeyApiClient apiKeyApiClient, WebhookApiClient webhookApiClient,
                             OnlineShopApiClient onlineShopApiClient) {
        this.onlineShopPanel = onlineShopApiClient == null ? null : new OnlineShopSettingsPanel(onlineShopApiClient);
        this.apiKeysPanel = new ApiKeysPanel(apiKeyApiClient);
        this.webhooksPanel = webhookApiClient == null ? null : new WebhooksPanel(webhookApiClient);
        setLayout(new GridBagLayout());
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(Spacing.LG, Spacing.LG, Spacing.LG, Spacing.LG));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        add(apiKeysPanel, c);
        if (webhooksPanel != null) {
            c.weighty = 2;
            c.insets = new Insets(Spacing.LG, 0, 0, 0);
            add(webhooksPanel, c);
        }
        if (onlineShopPanel != null) {
            c.weighty = 1;
            c.insets = new Insets(Spacing.LG, 0, 0, 0);
            add(onlineShopPanel, c);
        }
    }

    public void refresh() {
        apiKeysPanel.refresh();
        if (webhooksPanel != null) webhooksPanel.refresh();
        if (onlineShopPanel != null) onlineShopPanel.refresh();
    }
}
