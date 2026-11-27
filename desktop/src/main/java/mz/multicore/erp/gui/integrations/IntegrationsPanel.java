package mz.multicore.erp.gui.integrations;

import mz.multicore.erp.desktop.client.ApiKeyApiClient;
import mz.multicore.erp.gui.components.Spacing;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Configurações › Integrações (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.4): the public API keys now; the webhooks
 * (WH-UI-01, GOAL-014) join this same tab later.
 */
public class IntegrationsPanel extends JPanel {

    private final ApiKeysPanel apiKeysPanel;

    public IntegrationsPanel(ApiKeyApiClient apiKeyApiClient) {
        this.apiKeysPanel = new ApiKeysPanel(apiKeyApiClient);
        setLayout(new BorderLayout(0, Spacing.LG));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(Spacing.LG, Spacing.LG, Spacing.LG, Spacing.LG));
        add(apiKeysPanel, BorderLayout.CENTER);
    }

    public void refresh() {
        apiKeysPanel.refresh();
    }
}
