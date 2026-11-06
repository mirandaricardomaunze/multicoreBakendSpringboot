package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.QuickPeekPanel;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.inventory.dto.ProductBatchDTO;
import mz.multicore.erp.modules.inventory.dto.StockAlertDTO;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import mz.multicore.erp.gui.components.BatchDisplay;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Coordenador de apresentação e mapeamento dos alertas de stock e lotes a expirar.
 */
public final class StockAlertsViewCoordinator {

    public static final String MASK = "•••";

    public record StockAlerts(List<StockAlertDTO> outOfStock, List<ProductBatchDTO> expiring) {}

    private StockAlertsViewCoordinator() {}

    public static TableCellRenderer createExpirationRenderer(StockPanel owner, TableCellRenderer expBase) {
        return (t, v, sel, foc, row, col) -> {
            Component c = expBase.getTableCellRendererComponent(t, v, sel, foc, row, col);
            int modelRow = row >= 0 ? owner.alertsExpTable.convertRowIndexToModel(row) : -1;
            if (!sel && modelRow >= 0 && modelRow < owner.alertsExpModel.getRowCount()) {
                Object d = owner.alertsExpModel.getValueAt(modelRow, 5);
                long days = d instanceof Number n ? n.longValue() : 0;
                c.setForeground(days < 0 ? UIHelper.REJECTED_RED : UIHelper.PENDING_YELLOW);
            }
            return c;
        };
    }

    public static void populateOutOfStockPeek(QuickPeekPanel peek, StockPanel owner, int modelRow) {
        peek.setHeaderIcon("fas-ban", UIHelper.REJECTED_RED);
        peek.setTitle(String.valueOf(owner.alertsOutModel.getValueAt(modelRow, 1)));
        peek.setSubtitle("SKU: " + owner.alertsOutModel.getValueAt(modelRow, 0));
        peek.setStatus("ESGOTADO", UIHelper.REJECTED_RED);
        List<QuickPeekPanel.PeekItem> items = new ArrayList<>();
        items.add(new QuickPeekPanel.PeekItem("Artigo", peek.cell(owner.alertsOutModel.getValueAt(modelRow, 1), 1), false));
        items.add(new QuickPeekPanel.PeekItem("Stock Actual", peek.cell(owner.alertsOutModel.getValueAt(modelRow, 2), 2), true));
        peek.setItems(items);
    }

    public static void applyAlerts(StockPanel owner, StockAlerts alerts, boolean hide,
                                   JComboBox<String> outBusinessUnitCombo,
                                   JComboBox<String> expBusinessUnitCombo,
                                   JLabel outSummary) {
        var esgotados = alerts.outOfStock();
        owner.alertsOutModel.setRowCount(0);
        Set<String> outBus = new TreeSet<>();
        for (var a : esgotados) {
            String buName = a.businessUnitName() != null && !a.businessUnitName().isBlank() ? a.businessUnitName() : "—";
            if (!"—".equals(buName)) outBus.add(buName);
            owner.alertsOutModel.addRow(new Object[]{
                    a.sku(), a.name(),
                    hide ? MASK : (a.currentStock() == null ? "0" : a.currentStock().toPlainString()),
                    buName});
        }
        syncCombo(outBusinessUnitCombo, outBus);

        var expiring = alerts.expiring();
        owner.alertsExpModel.setRowCount(0);
        LocalDate today = LocalDate.now();
        long expired = 0, soon = 0;
        Set<String> expBus = new TreeSet<>();
        for (var b : expiring) {
            long days = b.expirationDate() == null ? 0 : ChronoUnit.DAYS.between(today, b.expirationDate());
            boolean isExpired = days < 0;
            if (isExpired) expired++; else soon++;
            if (b.businessUnitName() != null && !b.businessUnitName().isBlank()) expBus.add(b.businessUnitName());
            owner.alertsExpModel.addRow(new Object[]{
                    b.sku(), b.productName(), BatchDisplay.number(b.batchNumber()), b.warehouseName(),
                    b.expirationDate() == null ? "—" : b.expirationDate().toString(),
                    days, hide ? MASK : (b.quantity() == null ? "" : b.quantity().toPlainString()),
                    isExpired ? "Expirado" : "A expirar"});
        }

        if (owner.alertsSummary != null) {
            if (hide) {
                owner.alertsSummary.setForeground(UIHelper.TEXT_MUTED);
                owner.alertsSummary.setText("Quantidades ocultas — stock trancado (visível só para administradores).");
            } else if (expired == 0 && soon == 0) {
                owner.alertsSummary.setForeground(UIHelper.APPROVED_GREEN);
                owner.alertsSummary.setText("Nenhum lote expirado ou a expirar nos próximos 30 dias.");
            } else {
                owner.alertsSummary.setForeground(expired > 0 ? UIHelper.REJECTED_RED : UIHelper.PENDING_YELLOW);
                syncCombo(expBusinessUnitCombo, expBus);
                owner.alertsSummary.setText(String.format("%d lote(s) expirado(s)  ·  %d a expirar em ≤ 30 dias", expired, soon));
            }
        }

        if (outSummary != null) {
            if (hide) {
                outSummary.setForeground(UIHelper.TEXT_MUTED);
                outSummary.setText("Quantidades ocultas — stock trancado.");
            } else if (esgotados.isEmpty()) {
                outSummary.setForeground(UIHelper.APPROVED_GREEN);
                outSummary.setText("Nenhum artigo esgotado no armazém.");
            } else {
                outSummary.setForeground(UIHelper.REJECTED_RED);
                outSummary.setText(esgotados.size() + " artigo(s) actualmente sem stock.");
            }
        }
    }

    private static void syncCombo(JComboBox<String> combo, Set<String> items) {
        if (combo == null) return;
        Object prev = combo.getSelectedItem();
        combo.removeAllItems();
        combo.addItem("Todos os negócios");
        for (String b : items) combo.addItem(b);
        if (prev != null) combo.setSelectedItem(prev);
    }
}
