package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.modules.comercial.model.OrderKind;
import mz.multicore.erp.modules.inventory.dto.StockTransferDTO;

import javax.swing.*;
import java.util.List;

/**
 * Converter a reposição interna na transferência entre armazéns que a cumpre.
 *
 * <p>A via é lida da célula (que guarda o {@link OrderKind}), não de texto traduzido — a mesma
 * razão pela qual a coluna guarda o enum. E a decisão de deixar converter é do servidor: aqui só
 * se evita o percurso óbvio de erro, para o operador não descobrir pelo 400 que escolheu a linha
 * errada. Ver docs/REPOSICAO_INTERNA_SPEC.md §4.
 */
final class OrderToTransferAction {
    private OrderToTransferAction() {}

    static void convertSelected(ComercialPanel owner, ComercialApiClient api) {
        int row = TableFilter.selectedModelRow(owner.ordersTable);
        if (row < 0) {
            owner.showCommercialNotice(FeedbackType.WARNING, "Seleccione uma encomenda interna",
                    "Escolha uma encomenda de reposição para converter em transferência.");
            return;
        }

        Object kindCell = owner.ordersTableModel.getValueAt(row, ComercialPanel.ORDERS_COL_KIND);
        OrderKind kind = kindCell instanceof OrderKind k ? k : OrderKind.FORMAL_ORDER;
        if (!kind.usesWarehouseTransfer()) {
            owner.showCommercialNotice(FeedbackType.INFO, "Tipo de encomenda incompatível",
                    "Esta encomenda é " + kind.label().toLowerCase()
                            + ". Só a reposição interna se converte em transferência.");
            return;
        }

        Long orderId = (Long) owner.ordersTableModel.getValueAt(row, ComercialPanel.ORDERS_COL_ID);
        String orderNumber = String.valueOf(owner.ordersTableModel.getValueAt(row, 1));
        String destination = String.valueOf(owner.ordersTableModel.getValueAt(row, ComercialPanel.ORDERS_COL_ORIGIN));

        JTextField orderField = new JTextField(orderNumber);
        JTextField driverField = new JTextField();
        JTextField vehiclePlateField = new JTextField();
        JTextField responsibleField = new JTextField();
        JTextArea notesArea = new JTextArea(3, 28);
        for (JTextField f : List.of(orderField, driverField, vehiclePlateField, responsibleField)) {
            UIHelper.styleTextField(f);
        }
        orderField.setEditable(false);
        driverField.putClientProperty("JTextField.placeholderText", "Nome do motorista (obrigatório)");
        vehiclePlateField.putClientProperty("JTextField.placeholderText", "Matrícula do veículo (obrigatório, Ex: ABC-123-MC)");
        responsibleField.putClientProperty("JTextField.placeholderText", "Responsável pelo transporte (opcional)");
        UIHelper.styleTextArea(notesArea);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        JScrollPane notesScroll = new JScrollPane(notesArea);
        UIHelper.styleScrollPane(notesScroll);

        JPanel form = UIHelper.createDialogForm(
                "Encomenda:", orderField,
                "Motorista *:", driverField,
                "Matrícula do Veículo *:", vehiclePlateField,
                "Responsável:", responsibleField,
                "Observações:", notesScroll);

        StockTransferDTO[] created = new StockTransferDTO[1];
        ModernFormDialog dialog = new ModernFormDialog(SwingUtilities.getWindowAncestor(owner),
                "Converter em Transferência", "fas-dolly",
                "Origem, destino e artigos vêm da encomenda — falta só quem leva e em quê", form)
                .setConfirmButton("Criar Transferência", "fas-dolly")
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
                    return () -> created[0] = api.convertOrderToTransfer(orderId, responsible, plate, notes, driver, plate);
                });

        if (dialog.showDialog() && created[0] != null) {
            owner.showCommercialSuccess("Transferência " + created[0].transferNumber() + " criada a partir de "
                    + orderNumber + ("—".equals(destination) ? "" : ", com destino a " + destination)
                    + "; reveja e submeta o rascunho em Stock › Transferências.");
            owner.loadOrdersTable();
        }
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
