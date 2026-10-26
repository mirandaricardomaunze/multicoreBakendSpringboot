package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ReceiptDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Coordenador de visualização, Quick Peek e anulação de recibos comerciais.
 */
public final class ReceiptsViewCoordinator {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private ReceiptsViewCoordinator() {}

    public static void populateReceipts(DefaultTableModel model, JComboBox<String> businessUnit, List<ReceiptDTO> receipts) {
        model.setRowCount(0);
        Set<String> bus = new TreeSet<>();
        for (ReceiptDTO receipt : receipts) {
            String bu = (receipt.businessUnitName() != null && !receipt.businessUnitName().isBlank())
                    ? receipt.businessUnitName() : "Geral";
            bus.add(bu);
            model.addRow(new Object[]{
                    receipt.id(), receipt.receiptNumber(), bu, receipt.invoiceNumber(),
                    receipt.clientName(), receipt.amountPaid(), UIHelper.humanStatus(receipt.paymentMethod()),
                    receipt.status(), receipt.receiptDate().format(DATE_TIME)
            });
        }
        Object sel = businessUnit.getSelectedItem();
        businessUnit.removeAllItems();
        businessUnit.addItem("Todos os negócios");
        for (String b : bus) businessUnit.addItem(b);
        if (sel != null) businessUnit.setSelectedItem(sel);
    }

    public static void renderQuickPeek(QuickPeekPanel peek, int mr, DefaultTableModel model, Runnable onCancel) {
        if (mr < 0 || mr >= model.getRowCount()) return;
        peek.setTitle("Recibo " + model.getValueAt(mr, 1));
        peek.setSubtitle(String.valueOf(model.getValueAt(mr, 4)));
        peek.setItems(List.of(
                new QuickPeekPanel.PeekItem("Linha de Negócio", String.valueOf(model.getValueAt(mr, 2)), false),
                new QuickPeekPanel.PeekItem("Fatura Liquidada", String.valueOf(model.getValueAt(mr, 3)), false),
                new QuickPeekPanel.PeekItem("Cliente", String.valueOf(model.getValueAt(mr, 4)), false),
                new QuickPeekPanel.PeekItem("Data", String.valueOf(model.getValueAt(mr, 8)), false),
                new QuickPeekPanel.PeekItem("Método", String.valueOf(model.getValueAt(mr, 6)), false),
                new QuickPeekPanel.PeekItem("Montante Pago", String.valueOf(model.getValueAt(mr, 5)), true),
                new QuickPeekPanel.PeekItem("Estado", String.valueOf(model.getValueAt(mr, 7)), false)
        ));
        peek.getOpenFullButton().setText("Anular Recibo");
        peek.setOnOpenFullAction(v -> onCancel.run());
    }

    public static void cancelSelected(Component parent, JTable table, DefaultTableModel model,
                                      ComercialApiClient apiClient, InlineFeedbackPanel feedback,
                                      Runnable invoicesRefresh, Runnable onRefresh) {
        int row = TableFilter.selectedModelRow(table);
        if (row < 0) {
            feedback.show(FeedbackType.WARNING, "Seleccione um recibo", "Escolha um recibo na tabela para anular.", null, null);
            return;
        }
        Long id = (Long) model.getValueAt(row, 0);
        String number = String.valueOf(model.getValueAt(row, 1));
        String reason = UIHelper.promptRequiredText("Anular Recibo", "fas-ban", "Recibo " + number, "Motivo da anulação:");
        if (reason == null) return;

        UIHelper.runWithProgress(parent, "A anular recibo…", () -> {
            apiClient.cancelReceipt(id, reason);
            return null;
        }, ignored -> {
            ToastManager.success(parent, "Recibo " + number + " anulado; o estado da fatura foi actualizado.");
            if (invoicesRefresh != null) invoicesRefresh.run();
            if (onRefresh != null) onRefresh.run();
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível anular recibo", error.getMessage(), null, null));
    }
}
