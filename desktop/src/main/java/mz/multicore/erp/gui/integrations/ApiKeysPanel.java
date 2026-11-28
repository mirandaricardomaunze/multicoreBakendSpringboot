package mz.multicore.erp.gui.integrations;

import mz.multicore.erp.desktop.client.ApiKeyApiClient;
import mz.multicore.erp.desktop.session.SignedInUser;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.publicapi.dto.ApiKeyDTO;
import mz.multicore.erp.modules.publicapi.dto.CreatedApiKeyDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Configurações › Integrações › Chaves de API (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.4.1): the public API keys of
 * the active company, created with their permissions (secret shown once) and revoked. Only the ADMIN manages keys;
 * the backend decides, this only disables what the server would refuse.
 */
public class ApiKeysPanel extends JPanel {

    static final String ADMIN_ONLY = "Só o administrador gere as chaves de API.";

    private final ApiKeyApiClient apiKeyApiClient;
    private final DefaultTableModel keysModel = new DefaultTableModel(
            new String[]{"Nome", "Identificação", "Permissões", "Criada em / por", "Último uso", "Estado"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable keysTable = new JTable(keysModel);
    private final List<ApiKeyDTO> keys = new ArrayList<>();
    private final JLabel statusLabel = new JLabel();
    private final ModernButton newBtn = UIHelper.createSuccessButton("Nova chave");
    private final ModernButton revokeBtn = UIHelper.createDangerButton("Revogar");

    public ApiKeysPanel(ApiKeyApiClient apiKeyApiClient) {
        this.apiKeyApiClient = apiKeyApiClient;
        setLayout(new BorderLayout());
        setOpaque(false);
        add(buildKeysCard(), BorderLayout.CENTER);
        keysTable.getSelectionModel().addListSelectionListener(e -> updateActions());
        updateActions();
    }

    private JComponent buildKeysCard() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, Spacing.SM));
        card.setBorder(new EmptyBorder(Spacing.XL, Spacing.XL, Spacing.XL, Spacing.XL));

        UIHelper.styleTable(keysTable);
        UIHelper.ensureHeadersFit(keysTable);
        keysTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(keysTable);
        UIHelper.styleScrollPane(scroll);

        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newBtn.addActionListener(e -> createKey());
        revokeBtn.setIcon(UIHelper.icon("fas-ban", 14));
        revokeBtn.addActionListener(e -> revokeSelected());
        ModernButton refreshBtn = UIHelper.createRefreshButton(this::refresh);

        statusLabel.setFont(Typography.CAPTION);
        statusLabel.setForeground(UIHelper.TEXT_MUTED);

        card.add(UIHelper.tableCardTop("Chaves de API", statusLabel, newBtn, revokeBtn, refreshBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        JLabel hint = new JLabel("Cada chave dá a um sistema externo (loja online, contabilista) acesso à API pública "
                + "desta empresa. A chave só é mostrada quando é criada.");
        hint.setFont(Typography.CAPTION);
        hint.setForeground(UIHelper.TEXT_MUTED);
        card.add(hint, BorderLayout.SOUTH);
        return card;
    }

    /** Reloads the list; for a non-ADMIN the list is not requested (§4.4.1). */
    public void refresh() {
        if (!SignedInUser.isAdmin()) {
            applyKeys(List.of());
            statusLabel.setText(ADMIN_ONLY);
            return;
        }
        UIHelper.loadAsync(this, apiKeyApiClient::list, this::applyKeys,
                error -> showError("carregar as chaves de API", error));
    }

    void applyKeys(List<ApiKeyDTO> list) {
        keys.clear();
        if (list != null) keys.addAll(list);
        keysModel.setRowCount(0);
        long active = 0;
        for (ApiKeyDTO key : keys) {
            keysModel.addRow(new Object[]{key.name(), ApiKeyLabels.identification(key),
                    ApiKeyLabels.permissions(key.permissions()), ApiKeyLabels.created(key),
                    ApiKeyLabels.lastUse(key), ApiKeyLabels.state(key)});
            if (key.active()) active++;
        }
        statusLabel.setText(SignedInUser.isAdmin()
                ? active + " chave(s) activa(s) de " + keys.size()
                : ADMIN_ONLY);
        updateActions();
    }

    private void updateActions() {
        boolean admin = SignedInUser.isAdmin();
        ApiKeyDTO selected = selected();
        newBtn.setEnabled(admin);
        revokeBtn.setEnabled(admin && selected != null && selected.active());
    }

    private ApiKeyDTO selected() {
        int row = keysTable.getSelectedRow();
        return row < 0 ? null : keys.get(keysTable.convertRowIndexToModel(row));
    }

    private void createKey() {
        CreatedApiKeyDTO created = new NewApiKeyDialog(apiKeyApiClient).show();
        if (created == null) return;
        new ApiKeySecretDialog(created.secret()).show();
        refresh();
    }

    private void revokeSelected() {
        ApiKeyDTO key = selected();
        if (key == null || !key.active()) {
            ToastManager.show(this, FeedbackType.WARNING, "Seleccione uma chave activa.");
            return;
        }
        if (!UIHelper.confirm(this, revokeConfirmation(key), "Revogar chave de API")) return;
        UIHelper.loadAsync(this, () -> apiKeyApiClient.revoke(key.id()), revoked -> {
            ToastManager.show(this, FeedbackType.SUCCESS, "Chave " + revoked.name() + " revogada.");
            refresh();
        }, error -> showError("revogar a chave de API", error));
    }

    String revokeConfirmation(ApiKeyDTO key) {
        return "A chave " + key.name() + " (" + ApiKeyLabels.identification(key)
                + ") deixa de funcionar imediatamente. Revogar?";
    }

    private void showError(String action, Throwable error) {
        ToastManager.show(this, FeedbackType.ERROR, "Não foi possível " + action + ": " + error.getMessage());
    }

    JTable keysTable() { return keysTable; }
    JLabel statusLabel() { return statusLabel; }
    ModernButton newButton() { return newBtn; }
    ModernButton revokeButton() { return revokeBtn; }
}
