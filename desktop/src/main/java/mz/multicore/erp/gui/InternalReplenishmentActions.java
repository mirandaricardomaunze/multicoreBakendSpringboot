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
        JTextField driverField = new JTextField();
        JTextField vehiclePlateField = new JTextField();
        JTextField responsibleField = new JTextField();
        JTextArea notesArea = new JTextArea(3, 28);
        for (JTextField field : List.of(orderField, routeField, driverField, vehiclePlateField, responsibleField)) {
            UIHelper.styleTextField(field);
        }
        orderField.setEditable(false);
        routeField.setEditable(false);
        driverField.putClientProperty("JTextField.placeholderText", "Nome do motorista (obrigatório)");
        vehiclePlateField.putClientProperty("JTextField.placeholderText", "Matrícula do veículo (obrigatório, Ex: ABC-123-MC)");
        responsibleField.putClientProperty("JTextField.placeholderText", "Responsável pelo transporte (opcional)");

        JPanel form = UIHelper.createDialogForm(
                "Encomenda", orderField,
                "Percurso", routeField,
                "Motorista *", driverField,
                "Matrícula do Veículo *", vehiclePlateField,
                "Responsável", responsibleField,
                "Observações", new JScrollPane(notesArea));

        StockTransferDTO[] created = new StockTransferDTO[1];
        ModernFormDialog dialog = new ModernFormDialog(SwingUtilities.getWindowAncestor(owner),
                "Converter em Transferência", "fas-truck",
                "A mercadoria só sai do armazém quando a transferência for aprovada", form)
                .setConfirmButton("Criar Transferência", "fas-truck")
                .setOnSaveAsync(() -> {
                    String driver = blankToNull(driverField.getText());
                    String plate = blankToNull(vehiclePlateField.getText());
                    String responsible = blankToNull(responsibleField.getText());
                    String notes = blankToNull(notesArea.getText());
                    if (driver == null && responsible == null) {
                        driverField.requestFocusInWindow();
                        throw new IllegalArgumentException("O nome do motorista é obrigatório para emitir a guia de transferência.");
                    }
                    if (plate == null) {
                        vehiclePlateField.requestFocusInWindow();
                        throw new IllegalArgumentException("A matrícula do veículo é obrigatória para emitir a guia de transferência.");
                    }
                    return () -> created[0] = api.convertOrderToTransfer(order.id(), responsible, plate, notes, driver, plate);
                });
        if (!dialog.showDialog() || created[0] == null) return;
        owner.loadOrdersTable();
        owner.showCommercialSuccess("Transferência " + created[0].transferNumber() + " criada a partir de "
                + order.orderNumber() + "; reveja e submeta o rascunho em Stock → Transferências entre Armazéns.");
    }

    private static String route(OrderDTO order) {
        String destination = order.destinationWarehouseName() == null ? "—" : order.destinationWarehouseName();
        return "para " + destination;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
