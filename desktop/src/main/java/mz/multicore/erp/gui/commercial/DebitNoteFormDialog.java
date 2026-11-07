package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.desktop.client.DebitNoteApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.CreateDebitNoteLineRequest;
import mz.multicore.erp.modules.comercial.dto.CreateDebitNoteRequest;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Diálogo modal para emissão de Notas de Débito sobre fatura. */
public final class DebitNoteFormDialog {

    private static final String[] DEBIT_REASONS = {"FREIGHT", "SURCHARGE", "CORRECTION", "OTHER"};

    private DebitNoteFormDialog() {}

    public static void show(JComponent parent, DebitNoteApiClient debitApiClient, List<InvoiceDTO> invoices,
                            Runnable onCreated, Consumer<Throwable> onError) {
        JComboBox<String> invoice = new JComboBox<>();
        for (InvoiceDTO inv : invoices) invoice.addItem(inv.invoiceNumber() + " — " + inv.buyerName());
        UIHelper.styleComboBox(invoice);

        JComboBox<String> reason = new JComboBox<>(DEBIT_REASONS);
        UIHelper.styleComboBox(reason);

        JTextField description = new JTextField();
        UIHelper.styleTextField(description);

        DefaultTableModel lines = new DefaultTableModel(new String[]{"Descrição", "Valor", "IVA (0.16)"}, 0);
        lines.addRow(new Object[]{"Frete adicional", "0", "0"});
        JTable lineTable = new JTable(lines);
        UIHelper.styleTable(lineTable);

        ModernButton add = UIHelper.createAddLineButton();
        add.addActionListener(e -> lines.addRow(new Object[]{"", "0", "0"}));
        ModernButton remove = UIHelper.createDangerButton("Remover");
        remove.setIcon(UIHelper.icon("fas-minus", 14));
        remove.addActionListener(e -> {
            int sel = TableFilter.selectedModelRow(lineTable);
            if (sel >= 0) {
                lines.removeRow(sel);
            } else {
                ToastManager.show(UIHelper.mainWindow, FeedbackType.WARNING, "Seleccione na grelha a linha que pretende remover da nota.");
            }
        });

        JPanel tablePanel = new JPanel(new BorderLayout(0, 6));
        tablePanel.setOpaque(false);
        tablePanel.add(UIHelper.actionsBar(remove, add), BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(lineTable);
        UIHelper.styleScrollPane(scroll);
        tablePanel.add(scroll, BorderLayout.CENTER);

        JPanel form = UIHelper.createDialogForm("Fatura:", invoice, "Motivo:", reason, "Descrição:", description);
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        content.add(form, BorderLayout.NORTH);
        content.add(tablePanel, BorderLayout.CENTER);

        if (!new ModernFormDialog(UIHelper.mainWindow, "Emitir Nota de Débito", "fas-file-invoice-dollar",
                "Débito adicional sobre fatura", content).setConfirmButton("Emitir", "fas-check").showDialog()) return;
        if (lineTable.isEditing()) lineTable.getCellEditor().stopCellEditing();
        try {
            List<CreateDebitNoteLineRequest> requestedLines = new ArrayList<>();
            for (int row = 0; row < lines.getRowCount(); row++) {
                String text = String.valueOf(lines.getValueAt(row, 0)).trim();
                if (text.isEmpty()) throw new IllegalArgumentException("Descrição da linha não pode estar vazia.");
                requestedLines.add(new CreateDebitNoteLineRequest(text,
                        new BigDecimal(String.valueOf(lines.getValueAt(row, 1)).trim().replace(',', '.')),
                        new BigDecimal(String.valueOf(lines.getValueAt(row, 2)).trim().replace(',', '.'))));
            }
            if (requestedLines.isEmpty()) throw new IllegalArgumentException("Adicione pelo menos uma linha.");
            String desc = description.getText() == null || description.getText().isBlank() ? null : description.getText().trim();
            CreateDebitNoteRequest request = new CreateDebitNoteRequest(invoices.get(invoice.getSelectedIndex()).id(),
                    String.valueOf(reason.getSelectedItem()), desc, requestedLines);
            UIHelper.runWithProgress(parent, "A emitir nota de débito…", () -> debitApiClient.create(request), created -> {
                ToastManager.success(parent, "Nota " + created.noteNumber() + " emitida; pendente de aprovação.");
                onCreated.run();
            }, onError);
        } catch (RuntimeException error) {
            ToastManager.show(UIHelper.mainWindow, FeedbackType.ERROR, "Dados da nota de débito inválidos: " + error.getMessage());
        }
    }
}
