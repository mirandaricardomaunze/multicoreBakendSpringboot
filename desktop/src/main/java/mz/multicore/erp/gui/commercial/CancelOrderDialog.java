package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.ComercialPanel;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/** Pesquisa e cancelamento de encomendas ainda não faturadas. */
public final class CancelOrderDialog {
    private final JComponent owner;
    private final ComercialApiClient apiClient;
    private final Runnable ordersRefresh;

    public CancelOrderDialog(JComponent owner, ComercialApiClient apiClient, Runnable ordersRefresh) {
        this.owner = owner;
        this.apiClient = apiClient;
        this.ordersRefresh = ordersRefresh;
    }

    public void open() {
        UIHelper.loadAsync(owner, () -> apiClient.searchCancellableOrders(""),
                loaded -> show(new ArrayList<>(loaded)), error -> showError("carregar encomendas", error));
    }

    private void show(List<OrderDTO> orders) {
        if (orders.isEmpty()) {
            notice(FeedbackType.INFO, "Sem encomendas canceláveis",
                    "Apenas encomendas ainda não faturadas podem ser canceladas.");
            return;
        }
        JTextField search = new JTextField();
        JTextField reason = new JTextField();
        UIHelper.styleTextField(search);
        UIHelper.styleTextField(reason);
        JComboBox<String> combo = new JComboBox<>();
        UIHelper.styleComboBox(combo);
        Runnable rebuild = () -> {
            combo.removeAllItems();
            for (OrderDTO order : orders) {
                String state = "PENDING_APPROVAL".equals(order.status()) ? "por aprovar" : "aprovada";
                combo.addItem(order.orderNumber() + " — " + order.clientName() + " — " + order.totalAmount() + " MT (" + state + ")");
            }
            if (!orders.isEmpty()) combo.setSelectedIndex(0);
        };
        UIHelper.onTextChange(search, () -> {
            String query = search.getText();
            UIHelper.loadAsync(owner, () -> apiClient.searchCancellableOrders(query), loaded -> {
                orders.clear(); orders.addAll(loaded); rebuild.run();
            }, error -> showError("pesquisar encomendas", error));
        });
        rebuild.run();
        JPanel form = UIHelper.createDialogForm("Pesquisar (nº ou cliente):", search,
                "Encomenda a cancelar:", combo, "Motivo do cancelamento:", reason);
        if (!new ModernFormDialog(UIHelper.mainWindow, "Cancelar Encomenda", "fas-ban",
                "Anular uma encomenda pendente", form).setConfirmButton("Confirmar", "fas-check").showDialog()) return;
        int index = combo.getSelectedIndex();
        String text = reason.getText().trim();
        if (index < 0 || index >= orders.size()) return;
        if (text.isEmpty()) {
            notice(FeedbackType.ERROR, "Motivo obrigatório", "Indique o motivo do cancelamento.");
            return;
        }
        OrderDTO selected = orders.get(index);
        UIHelper.runWithProgress(owner, "A cancelar encomenda…", () -> {
            apiClient.cancelOrder(selected.id(), text);
            return null;
        }, ignored -> {
            if (owner instanceof ComercialPanel panel) panel.showCommercialSuccess(
                    "Encomenda " + selected.orderNumber() + " cancelada.");
            ordersRefresh.run();
        }, error -> showError("cancelar encomenda", error));
    }

    private void showError(String action, Throwable error) {
        notice(FeedbackType.ERROR, "Não foi possível " + action, error.getMessage());
    }
    private void notice(FeedbackType type, String title, String message) {
        if (owner instanceof ComercialPanel panel) panel.showCommercialNotice(type, title, message);
    }
}
