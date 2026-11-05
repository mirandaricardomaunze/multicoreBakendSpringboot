package mz.multicore.erp.gui.inventory;

import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.inventory.dto.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Coordenador de apresentação, renderização tabular, toolbar e KPIs de inventário físico. */
public final class PhysicalInventoryViewCoordinator {

    private PhysicalInventoryViewCoordinator() {}

    public static void populateTable(DefaultTableModel tableModel, InventorySessionDTO session) {
        if (tableModel == null) return;
        tableModel.setRowCount(0);
        if (session == null || session.items() == null) return;

        for (InventoryItemDTO item : session.items()) {
            boolean blind = session.blindCounting();
            tableModel.addRow(new Object[]{
                    item.productCode(), item.productName(),
                    blind ? "***" : String.format("%.2f", item.expectedQuantity()),
                    String.format("%.2f", item.countedQuantity()),
                    blind ? "***" : String.format("%+.2f", item.difference()),
                    String.format("%.2f", item.unitCost()),
                    blind ? "***" : String.format("%+.2f", item.financialImpact()),
                    item.notes() != null ? item.notes() : ""
            });
        }
    }

    public static void populatePeek(QuickPeekPanel peek, DefaultTableModel tableModel, int row) {
        if (peek == null || tableModel == null || row < 0 || row >= tableModel.getRowCount()) return;
        peek.setHeaderIcon("fas-boxes", UIHelper.ACCENT_BLUE);
        peek.setTitle(String.valueOf(tableModel.getValueAt(row, 1)));
        peek.setSubtitle("SKU: " + tableModel.getValueAt(row, 0));
        peek.setStatus(String.valueOf(tableModel.getValueAt(row, 6)), UIHelper.ACCENT_BLUE);
        peek.setItems(List.of(
                new QuickPeekPanel.PeekItem("Qtd Sistema", String.valueOf(tableModel.getValueAt(row, 2)), false),
                new QuickPeekPanel.PeekItem("Qtd Contada", String.valueOf(tableModel.getValueAt(row, 3)), false),
                new QuickPeekPanel.PeekItem("Divergência", String.valueOf(tableModel.getValueAt(row, 4)), true),
                new QuickPeekPanel.PeekItem("Impacto Financeiro", String.valueOf(tableModel.getValueAt(row, 5)), true)
        ));
    }

    public static JPanel buildToolbar(
            JComboBox<String> sessionCombo, JLabel statusLabel, JLabel countModeLabel,
            JTextField searchField, ModernButton refreshBtn, JTextField barcodeField,
            JTextField manualQty, ModernButton recordBtn, ModernButton peekBtn
    ) {
        // Duas linhas: lado a lado os grupos não cabem em 1366 px e o BorderLayout sobrepunha-os.
        JPanel bar = new JPanel(new GridLayout(2, 1, 0, 6));
        bar.setOpaque(false);
        JPanel sessionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        sessionRow.setOpaque(false);
        sessionRow.add(TableFilter.label("Sessão:", "fas-clipboard-list"));
        sessionRow.add(sessionCombo); sessionRow.add(statusLabel); sessionRow.add(countModeLabel);
        sessionRow.add(searchField); sessionRow.add(refreshBtn);
        bar.add(sessionRow);

        JPanel scanRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        scanRow.setOpaque(false);
        scanRow.add(TableFilter.label("Bipar código / SKU:", "fas-barcode"));
        scanRow.add(barcodeField); scanRow.add(TableFilter.label("Qtd:"));
        scanRow.add(manualQty); scanRow.add(recordBtn); scanRow.add(peekBtn);
        bar.add(scanRow);
        return bar;
    }

    public static void updateKpiAndButtons(
            InventorySessionDTO session, JLabel total, JLabel counted, JLabel surplus, JLabel deficit,
            JLabel status, JLabel countMode, ActionMenuButton sessionMenu, ModernButton printPdf,
            JTextField barcodeField, JTextField manualQty, ModernButton recordBtn
    ) {
        if (session == null) {
            total.setText("0"); counted.setText("0"); surplus.setText("0,00 MT"); deficit.setText("0,00 MT");
            status.setText(" [Sem Sessão] "); countMode.setText("");
            enableControls(false, barcodeField, manualQty, recordBtn);
            return;
        }
        total.setText(String.valueOf(session.totalItems()));
        counted.setText(String.valueOf(session.itemsCounted()));
        surplus.setText(String.format("%,.2f MT", session.totalSurplusValue()));
        deficit.setText(String.format("%,.2f MT", session.totalDeficitValue()));
        status.setText(" [" + session.status().getDescription() + "] ");
        countMode.setText(session.blindCounting() ? "(Contagem Cega)" : "(Contagem Aberta)");

        boolean isDraft = session.status() == InventoryStatus.DRAFT;
        boolean inProgress = session.status() == InventoryStatus.IN_PROGRESS;
        boolean isClosed = session.status() == InventoryStatus.CLOSED;

        sessionMenu.setActionEnabled(0, isDraft);
        sessionMenu.setActionEnabled(1, inProgress || isDraft);
        sessionMenu.setActionEnabled(2, !isClosed);
        sessionMenu.setEnabled(!isClosed);
        printPdf.setEnabled(true);
        enableControls(inProgress || isDraft, barcodeField, manualQty, recordBtn);
    }

    public static void enableControls(boolean enable, JTextField barcode, JTextField qty, ModernButton recordBtn) {
        if (barcode != null) barcode.setEnabled(enable);
        if (qty != null) qty.setEnabled(enable);
        if (recordBtn != null) recordBtn.setEnabled(enable);
    }
}
