package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;
import mz.multicore.erp.modules.inventory.dto.StockTransferDTO;

import javax.swing.*;
import java.util.List;

/**
 * Converter uma reposição interna na transferência que a cumpre.
 *
 * <p>A origem, o destino e os artigos vêm todos da encomenda — o que falta perguntar é só quem
 * leva a mercadoria e em quê. Ver {@code docs/REPOSICAO_INTERNA_SPEC.md} §4.
 */
final class InternalReplenishmentActions {
    private InternalReplenishmentActions() {}

    static void showConvertDialog(ComercialPanel owner, ComercialApiClient api, OrderDTO order) {
        JTextField orderField = new JTextField(order.orderNumber());
        JTextField routeField = new JTextField(route(order));
        JTextField responsibleField = new JTextField();
        JTextField vehicleField = new JTextField();
        JTextArea notesArea = new JTextArea(3, 28);
        for (JTextField field : List.of(orderField, routeField, responsibleField, vehicleField)) {
            UIHelper.styleTextField(field);
        }
        orderField.setEditable(false);
        routeField.setEditable(false);

        JPanel form = UIHelper.createDialogForm(
                "Encomenda", orderField,
                "Percurso", routeField,
                "Responsável pelo transporte", responsibleField,
                "Viatura", vehicleField,
                "Observações", new JScrollPane(notesArea));

        StockTransferDTO[] created = new StockTransferDTO[1];
        ModernFormDialog dialog = new ModernFormDialog(SwingUtilities.getWindowAncestor(owner),
                "Converter em Transferência", "fas-truck",
                "A mercadoria só sai do armazém quando a transferência for aprovada", form)
                .setConfirmButton("Criar Transferência", "fas-truck")
                .setOnSaveAsync(() -> {
                    String responsible = blankToNull(responsibleField.getText());
                    String vehicle = blankToNull(vehicleField.getText());
                    String notes = blankToNull(notesArea.getText());
                    return () -> created[0] = api.convertOrderToTransfer(order.id(), responsible, vehicle, notes);
                });
        if (!dialog.showDialog() || created[0] == null) return;
        owner.loadOrdersTable();
        JOptionPane.showMessageDialog(owner,
                "Transferência " + created[0].transferNumber() + " criada a partir de " + order.orderNumber() + ".\n\n"
                        + "A mercadoria ainda não se moveu: aprove a transferência em "
                        + "Stock → Transferências entre Armazéns.",
                "Sucesso", JOptionPane.INFORMATION_MESSAGE);
    }

    private static String route(OrderDTO order) {
        String destination = order.destinationWarehouseName() == null ? "—" : order.destinationWarehouseName();
        return "para " + destination;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
