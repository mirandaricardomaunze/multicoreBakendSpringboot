package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.CreateInvoiceLineRequest;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;
import mz.multicore.erp.modules.comercial.dto.OrderLineDTO;

import javax.swing.JComboBox;
import java.util.Locale;
import java.util.Set;

/**
 * Coordenador do ciclo de vida de encomendas (abertura, carregamento assíncrono e validação de estados).
 */
final class CommercialOrderLifecycleCoordinator {

    private CommercialOrderLifecycleCoordinator() {}

    static void openSelected(ComercialPanel owner) {
        int row = TableFilter.selectedModelRow(owner.ordersTable);
        if (row < 0) {
            owner.showCommercialNotice(FeedbackType.WARNING, "Seleccione uma encomenda",
                    "Escolha na tabela a encomenda que pretende editar ou consultar.");
            return;
        }
        Long orderId = (Long) owner.ordersTableModel.getValueAt(row, ComercialPanel.ORDERS_COL_ID);
        UIHelper.loadAsync(owner, () -> owner.comercialApiClient.getOrderById(orderId), order -> {
            if (!isEditableStatus(order.status())) {
                owner.showCommercialNotice(FeedbackType.INFO, "Encomenda em consulta",
                        "Esta encomenda está em \"" + order.statusLabel() + "\" e já não pode ser alterada. Foram abertos os detalhes.");
                owner.orderDetailsDialog.open(order.id());
                return;
            }
            CommercialOrderEditorActions.load(owner, order);
        }, error -> owner.showCommercialError("abrir encomenda", error));
    }

    static void selectWarehouse(ComercialPanel owner, JComboBox<String> combo, Long warehouseId) {
        if (combo == null || warehouseId == null) return;
        for (int index = 0; index < owner.warehousesList.size(); index++) {
            if (warehouseId.equals(owner.warehousesList.get(index).id())) {
                combo.setSelectedIndex(index);
                return;
            }
        }
    }

    static boolean isEditableStatus(String status) {
        return status != null && Set.of("PENDING_APPROVAL", "PENDING", "AWAITING_SEPARATION")
                .contains(status.toUpperCase(Locale.ROOT));
    }
}
