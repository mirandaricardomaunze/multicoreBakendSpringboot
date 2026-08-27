package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.UIHelper;
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
            JOptionPane.showMessageDialog(owner,
                    "Não há encomendas canceláveis. Apenas encomendas ainda não faturadas podem ser canceladas.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
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
            JOptionPane.showMessageDialog(owner, "Indique o motivo do cancelamento.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        OrderDTO selected = orders.get(index);
        UIHelper.runWithProgress(owner, "A cancelar encomenda…", () -> {
            apiClient.cancelOrder(selected.id(), text);
            return null;
        }, ignored -> {
            JOptionPane.showMessageDialog(owner, "Encomenda " + selected.orderNumber() + " cancelada.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            ordersRefresh.run();
        }, error -> showError("cancelar encomenda", error));
    }

    private void showError(String action, Throwable error) {
        JOptionPane.showMessageDialog(owner, "Não foi possível " + action + ": " + error.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
    }
}
