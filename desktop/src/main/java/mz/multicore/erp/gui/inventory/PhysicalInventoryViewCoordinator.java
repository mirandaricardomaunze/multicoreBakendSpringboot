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
                new QuickPeekPanel.PeekItem("Qtd Sistema", peek.cell(tableModel.getValueAt(row, 2), 2), false),
                new QuickPeekPanel.PeekItem("Qtd Contada", peek.cell(tableModel.getValueAt(row, 3), 3), false),
                new QuickPeekPanel.PeekItem("Divergência", peek.cell(tableModel.getValueAt(row, 4), 4), true),
                new QuickPeekPanel.PeekItem("Impacto Financeiro", peek.cell(tableModel.getValueAt(row, 5), 5), true)
        ));
    }

    public static JPanel buildToolbar(
            JComboBox<String> sessionCombo, JLabel statusLabel, JLabel countModeLabel,
            JTextField searchField, JTextField barcodeField,
            JTextField manualQty, ModernButton recordBtn, ModernButton peekBtn
    ) {
        // Uma só linha, como nos outros ecrãs: sem espaço, a pesquisa e o selector encolhem em vez de sair da janela.
        return TableFilter.bar(
                TableFilter.label("Sessão:", "fas-clipboard-list"), sessionCombo, statusLabel, countModeLabel,
                searchField, TableFilter.label("Bipar:", "fas-barcode"), barcodeField,
                TableFilter.label("Qtd:"), manualQty, recordBtn, peekBtn);
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
        countMode.setText(session.blindCounting() ? "(Cega)" : "(Aberta)");
        countMode.setToolTipText(session.blindCounting() ? "Contagem cega: quantidades esperadas ocultas" : "Contagem aberta");

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
