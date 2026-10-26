package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.InlineFeedbackPanel;
import mz.multicore.erp.gui.components.QuickPeekPanel;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;

import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Coordenador de operações operacionais e diálogos da gestão de clientes.
 */
public class ClientesOperationsCoordinator {

    private final ComercialApiClient comercialApiClient;

    public ClientesOperationsCoordinator(ComercialApiClient comercialApiClient) {
        this.comercialApiClient = comercialApiClient;
    }

    public void openClientDialog(Component parent, ClientDTO existing, Runnable onSuccess) {
        ClientFormDialog.show(parent, existing, comercialApiClient, onSuccess);
    }

    public void deleteClient(Component parent, ClientDTO client, InlineFeedbackPanel feedback, Runnable onSuccess) {
        if (client == null) return;
        boolean confirmed = UIHelper.confirm(parent,
                "Eliminar o cliente '" + client.name() + "'? Esta ação não pode ser revertida.",
                "Confirmar Eliminação");
        if (!confirmed) return;

        try {
            comercialApiClient.deleteClient(client.id());
            ToastManager.success(parent, "Cliente eliminado.");
            if (onSuccess != null) onSuccess.run();
        } catch (Exception ex) {
            if (feedback != null) {
                feedback.show(FeedbackType.ERROR, "Não foi possível eliminar o cliente", ex.getMessage(), null, null);
            }
        }
    }

    public void openDetail(Component parent, ClientDTO client, Runnable onEdit, Runnable onStatement) {
        if (client == null) return;
        new CustomerDetailDialog(parent, client, onEdit, onStatement).show();
    }

    public static void populatePeek(QuickPeekPanel peek, DefaultTableModel model, int mr, Runnable onOpenFull) {
        if (mr < 0 || mr >= model.getRowCount()) return;
        String id = String.valueOf(model.getValueAt(mr, 0));
        String name = String.valueOf(model.getValueAt(mr, 1));
        String nuit = String.valueOf(model.getValueAt(mr, 2));
        String email = String.valueOf(model.getValueAt(mr, 3));
        String address = String.valueOf(model.getValueAt(mr, 4));
        String term = String.valueOf(model.getValueAt(mr, 5));
        String limit = String.valueOf(model.getValueAt(mr, 6));

        peek.setTitle(name);
        peek.setSubtitle("NUIT: " + nuit);
        peek.setItems(List.of(
                new QuickPeekPanel.PeekItem("ID", id, false),
                new QuickPeekPanel.PeekItem("Email", email, false),
                new QuickPeekPanel.PeekItem("Endereço", address, false),
                new QuickPeekPanel.PeekItem("Prazo de Pagamento", term + " dias", false),
                new QuickPeekPanel.PeekItem("Limite de Crédito", limit, true)
        ));
        peek.getOpenFullButton().setText("Ver Ficha");
        peek.setOnOpenFullAction(v -> onOpenFull.run());
    }

    public static Object formatTerms(int days) {
        return days == 0 ? "Pronto pagamento" : days;
    }

    public static String formatCreditLimit(BigDecimal limit) {
        return limit == null ? "Sem limite" : String.format("%,.2f MT", limit);
    }
}
