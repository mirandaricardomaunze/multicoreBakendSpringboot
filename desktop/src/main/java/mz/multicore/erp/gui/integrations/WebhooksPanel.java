package mz.multicore.erp.gui.integrations;

import mz.multicore.erp.desktop.client.WebhookApiClient;
import mz.multicore.erp.desktop.session.SignedInUser;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.webhooks.dto.CreatedWebhookSubscriptionDTO;
import mz.multicore.erp.modules.webhooks.dto.WebhookDeliveryDTO;
import mz.multicore.erp.modules.webhooks.dto.WebhookSubscriptionDTO;
import mz.multicore.erp.modules.webhooks.model.WebhookDeliveryStatus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Configurações › Integrações › Webhooks (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.4.2): the webhook subscriptions of
 * the active company (secret shown once), switched on/off, and the delivery log of the selected one, with resend of
 * a failed delivery. Only the ADMIN manages webhooks; the backend decides, this only disables what it would refuse.
 */
public class WebhooksPanel extends JPanel {

    static final String ADMIN_ONLY = "Só o administrador gere os webhooks.";
    static final String ALL_STATES = "Todos";

    private final WebhookApiClient webhookApiClient;
    private final DefaultTableModel subscriptionsModel = readOnlyModel(
            "URL", "Eventos", "Falhas seguidas", "Criada em / por", "Estado");
    private final DefaultTableModel deliveriesModel = readOnlyModel("Evento", "Estado", "Tentativas", "Último código",
            "Duração", "Último erro", "Próxima tentativa", "Criada em", "Entregue em");
    private final JTable subscriptionsTable = new JTable(subscriptionsModel);
    private final JTable deliveriesTable = new JTable(deliveriesModel);
    private final List<WebhookSubscriptionDTO> subscriptions = new ArrayList<>();
    private final List<WebhookDeliveryDTO> deliveries = new ArrayList<>();
    private final JLabel statusLabel = new JLabel();
    private final JComboBox<String> statusFilter = TableFilter.combo(ALL_STATES,
            WebhookLabels.status(WebhookDeliveryStatus.PENDING), WebhookLabels.status(WebhookDeliveryStatus.DELIVERED),
            WebhookLabels.status(WebhookDeliveryStatus.FAILED));
    private final ModernButton newBtn = UIHelper.createSuccessButton("Novo webhook");
    private final ModernButton toggleBtn = UIHelper.createSecondaryButton("Desactivar");
    private final ModernButton resendBtn = UIHelper.createSecondaryButton("Reenviar");

    public WebhooksPanel(WebhookApiClient webhookApiClient) {
        this.webhookApiClient = webhookApiClient;
        setLayout(new GridLayout(2, 1, 0, Spacing.LG));
        setOpaque(false);
        add(buildSubscriptionsCard());
        add(buildDeliveriesCard());
        subscriptionsTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            updateActions();
            loadDeliveries();
        });
        deliveriesTable.getSelectionModel().addListSelectionListener(e -> updateActions());
        statusFilter.addActionListener(e -> loadDeliveries());
        updateActions();
    }

    private JComponent buildSubscriptionsCard() {
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newBtn.addActionListener(e -> createWebhook());
        toggleBtn.setIcon(UIHelper.icon("fas-power-off", 14));
        toggleBtn.addActionListener(e -> toggleSelected());
        ModernButton refreshBtn = UIHelper.createRefreshButton(this::refresh);
        statusLabel.setFont(Typography.CAPTION);
        statusLabel.setForeground(UIHelper.TEXT_MUTED);

        ModernPanel card = tableCard(subscriptionsTable,
                UIHelper.tableCardTop("Webhooks", statusLabel, newBtn, toggleBtn, refreshBtn));
        JLabel title = new JLabel("Cada webhook avisa um sistema externo, com assinatura, quando há uma factura, nota "
                + "de crédito, mudança de estado de encomenda ou um artigo esgota/volta.");
        title.setFont(Typography.CAPTION);
        title.setForeground(UIHelper.TEXT_MUTED);
        title.setIcon(UIHelper.icon("fas-satellite-dish", 14, UIHelper.TEXT_MUTED));
        card.add(title, BorderLayout.SOUTH);
        return card;
    }

    private JComponent buildDeliveriesCard() {
        resendBtn.setIcon(UIHelper.icon("fas-redo", 14));
        resendBtn.addActionListener(e -> resendSelected());
        return tableCard(deliveriesTable, UIHelper.tableCardTop("Registo de entregas", statusFilter, resendBtn));
    }

    private static ModernPanel tableCard(JTable table, JComponent top) {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, Spacing.SM));
        card.setBorder(new EmptyBorder(Spacing.XL, Spacing.XL, Spacing.XL, Spacing.XL));
        UIHelper.styleTable(table);
        UIHelper.ensureHeadersFit(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        card.add(top, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    /** Reloads the subscriptions; for a non-ADMIN nothing is requested (§4.4.2). */
    public void refresh() {
        if (!SignedInUser.isAdmin()) {
            applySubscriptions(List.of());
            return;
        }
        UIHelper.loadAsync(this, webhookApiClient::list, this::applySubscriptions,
                error -> showError("carregar os webhooks", error));
    }

    void applySubscriptions(List<WebhookSubscriptionDTO> list) {
        subscriptions.clear();
        if (list != null) subscriptions.addAll(list);
        subscriptionsModel.setRowCount(0);
        long active = 0;
        for (WebhookSubscriptionDTO subscription : subscriptions) {
            subscriptionsModel.addRow(new Object[]{subscription.url(), WebhookLabels.events(subscription.events()),
                    subscription.consecutiveFailures(), WebhookLabels.created(subscription),
                    WebhookLabels.state(subscription)});
            if (subscription.active()) active++;
        }
        statusLabel.setText(SignedInUser.isAdmin()
                ? active + " webhook(s) activo(s) de " + subscriptions.size()
                : ADMIN_ONLY);
        applyDeliveries(List.of());
        updateActions();
    }

    void applyDeliveries(List<WebhookDeliveryDTO> list) {
        deliveries.clear();
        if (list != null) deliveries.addAll(list);
        deliveriesModel.setRowCount(0);
        deliveries.forEach(delivery -> deliveriesModel.addRow(WebhookLabels.deliveryRow(delivery)));
        updateActions();
    }

    private void loadDeliveries() {
        WebhookSubscriptionDTO subscription = selectedSubscription();
        if (subscription == null || !SignedInUser.isAdmin()) {
            applyDeliveries(List.of());
            return;
        }
        WebhookDeliveryStatus status = selectedStatusFilter();
        UIHelper.loadAsync(this, () -> webhookApiClient.deliveries(subscription.id(), status), this::applyDeliveries,
                error -> showError("carregar o registo de entregas", error));
    }

    /** The state chosen in the filter, or {@code null} for "Todos". */
    WebhookDeliveryStatus selectedStatusFilter() {
        int index = statusFilter.getSelectedIndex();
        return index <= 0 ? null : WebhookDeliveryStatus.values()[index - 1];
    }

    private void updateActions() {
        boolean admin = SignedInUser.isAdmin();
        WebhookSubscriptionDTO subscription = selectedSubscription();
        WebhookDeliveryDTO delivery = selectedDelivery();
        newBtn.setEnabled(admin);
        toggleBtn.setText(subscription != null && !subscription.active() ? "Activar" : "Desactivar");
        toggleBtn.setEnabled(admin && subscription != null);
        resendBtn.setEnabled(admin && delivery != null && delivery.status() == WebhookDeliveryStatus.FAILED);
    }

    private WebhookSubscriptionDTO selectedSubscription() {
        int row = subscriptionsTable.getSelectedRow();
        return row < 0 ? null : subscriptions.get(subscriptionsTable.convertRowIndexToModel(row));
    }

    private WebhookDeliveryDTO selectedDelivery() {
        int row = deliveriesTable.getSelectedRow();
        return row < 0 ? null : deliveries.get(deliveriesTable.convertRowIndexToModel(row));
    }

    private void createWebhook() {
        CreatedWebhookSubscriptionDTO created = new NewWebhookDialog(webhookApiClient).show();
        if (created == null) return;
        ApiKeySecretDialog.forWebhook(created.secret()).show();
        refresh();
    }

    private void toggleSelected() {
        WebhookSubscriptionDTO subscription = selectedSubscription();
        if (subscription == null) {
            ToastManager.show(this, FeedbackType.WARNING, "Seleccione um webhook.");
            return;
        }
        boolean deactivate = subscription.active();
        if (deactivate && !UIHelper.confirm(this, deactivateConfirmation(subscription), "Desactivar webhook")) return;
        UIHelper.loadAsync(this, () -> deactivate
                ? webhookApiClient.deactivate(subscription.id())
                : webhookApiClient.activate(subscription.id()), changed -> {
            ToastManager.show(this, FeedbackType.SUCCESS,
                    "Webhook " + changed.url() + (deactivate ? " desactivado." : " activado."));
            refresh();
        }, error -> showError(deactivate ? "desactivar o webhook" : "activar o webhook", error));
    }

    private void resendSelected() {
        WebhookDeliveryDTO delivery = selectedDelivery();
        if (delivery == null || delivery.status() != WebhookDeliveryStatus.FAILED) {
            ToastManager.show(this, FeedbackType.WARNING, "Seleccione uma entrega falhada.");
            return;
        }
        if (!UIHelper.confirm(this, resendConfirmation(delivery), "Reenviar entrega")) return;
        UIHelper.loadAsync(this, () -> webhookApiClient.resend(delivery.id()), resent -> {
            ToastManager.show(this, FeedbackType.SUCCESS, "Entrega " + resent.eventId() + " volta a ser enviada.");
            loadDeliveries();
        }, error -> showError("reenviar a entrega", error));
    }

    String deactivateConfirmation(WebhookSubscriptionDTO subscription) {
        return "O webhook " + subscription.url() + " deixa de receber avisos. Desactivar?";
    }

    String resendConfirmation(WebhookDeliveryDTO delivery) {
        return "A entrega " + delivery.eventId() + " volta a ser enviada. Reenviar?";
    }

    private void showError(String action, Throwable error) {
        ToastManager.show(this, FeedbackType.ERROR, "Não foi possível " + action + ": " + error.getMessage());
    }

    private static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    JTable subscriptionsTable() { return subscriptionsTable; }
    JTable deliveriesTable() { return deliveriesTable; }
    JLabel statusLabel() { return statusLabel; }
    JComboBox<String> statusFilter() { return statusFilter; }
    ModernButton newButton() { return newBtn; }
    ModernButton toggleButton() { return toggleBtn; }
    ModernButton resendButton() { return resendBtn; }
}
