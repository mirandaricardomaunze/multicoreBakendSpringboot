package com.phcpro.gui;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.gui.components.*;
import com.phcpro.modules.inventory.dto.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/** Alertas de rupturas e validades do stock. */
final class StockAlertsPanel {
    private static final String MASK = "•••";
    private final StockPanel owner;
    StockAlertsPanel(StockPanel owner) { this.owner = owner; }

    public JPanel buildPanel() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Alertas de Stock"), BorderLayout.WEST);
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Actualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshBtn.addActionListener(e -> refresh());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(refreshBtn);
        header.add(actions, BorderLayout.EAST);

        owner.alertsSummary = new JLabel(" ");
        owner.alertsSummary.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        owner.alertsSummary.setForeground(UIHelper.TEXT_MUTED);
        owner.alertsSummary.setBorder(new EmptyBorder(2, 2, 0, 0));

        JPanel topStack = new JPanel(new BorderLayout(0, 8));
        topStack.setOpaque(false);
        topStack.add(header, BorderLayout.NORTH);
        topStack.add(owner.alertsSummary, BorderLayout.SOUTH);
        tab.add(topStack, BorderLayout.NORTH);

        JTabbedPane sub = new JTabbedPane();
        UIHelper.styleTabbedPanePHC(sub);

        // Sub-aba: produtos esgotados (com pesquisa)
        String[] outCols = {"Artigo (SKU)", "Nome do Artigo", "Stock"};
        owner.alertsOutModel = new DefaultTableModel(outCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        owner.alertsOutTable = new JTable(owner.alertsOutModel);
        UIHelper.styleTable(owner.alertsOutTable);
        ModernPanel outCard = new ModernPanel(16);
        outCard.setLayout(new BorderLayout());
        outCard.setBorder(new EmptyBorder(15, 15, 15, 15));
        JScrollPane outScroll = new JScrollPane(owner.alertsOutTable);
        UIHelper.styleScrollPane(outScroll);
        outCard.add(outScroll, BorderLayout.CENTER);
        JTextField outSearch = TableFilter.searchField("SKU ou nome…");
        TableFilter.install(owner.alertsOutTable, outSearch);
        JPanel outWrap = new JPanel(new BorderLayout(0, 8));
        outWrap.setOpaque(false);
        outWrap.add(TableFilter.bar(outSearch), BorderLayout.NORTH);
        outWrap.add(outCard, BorderLayout.CENTER);
        sub.addTab("Esgotados", UIHelper.icon("fas-ban", 15, UIHelper.TEXT_LIGHT), outWrap);

        // Sub-aba: validades (expirados / a expirar) — com pesquisa + filtro de estado
        String[] expCols = {"SKU", "Nome do Artigo", "Nº Lote", "Armazém", "Validade", "Dias", "Qtd", "Estado"};
        owner.alertsExpModel = new DefaultTableModel(expCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        owner.alertsExpTable = new JTable(owner.alertsExpModel);
        UIHelper.styleTable(owner.alertsExpTable);
        // Vermelho para expirados, amarelo para a expirar (coluna Dias < 0 vs ≥ 0). Converte o índice
        // da vista para o modelo, porque o filtro instala um TableRowSorter.
        javax.swing.table.TableCellRenderer expBase = owner.alertsExpTable.getDefaultRenderer(Object.class);
        owner.alertsExpTable.setDefaultRenderer(Object.class, (t, v, sel, foc, row, col) -> {
            java.awt.Component c = expBase.getTableCellRendererComponent(t, v, sel, foc, row, col);
            int modelRow = row >= 0 ? owner.alertsExpTable.convertRowIndexToModel(row) : -1;
            if (!sel && modelRow >= 0 && modelRow < owner.alertsExpModel.getRowCount()) {
                Object d = owner.alertsExpModel.getValueAt(modelRow, 5);
                long days = d instanceof Number n ? n.longValue() : 0;
                c.setForeground(days < 0 ? UIHelper.REJECTED_RED : UIHelper.PENDING_YELLOW);
            }
            return c;
        });
        ModernPanel expCard = new ModernPanel(16);
        expCard.setLayout(new BorderLayout());
        expCard.setBorder(new EmptyBorder(15, 15, 15, 15));
        JScrollPane expScroll = new JScrollPane(owner.alertsExpTable);
        UIHelper.styleScrollPane(expScroll);
        expCard.add(expScroll, BorderLayout.CENTER);
        JTextField expSearch = TableFilter.searchField("SKU, nome ou lote…");
        JComboBox<String> expEstado = TableFilter.combo("Todos", "Expirado", "A expirar");
        TableFilter.install(owner.alertsExpTable, expSearch, new TableFilter.ColumnFilter(expEstado, 7));
        JPanel expWrap = new JPanel(new BorderLayout(0, 8));
        expWrap.setOpaque(false);
        expWrap.add(TableFilter.bar(expSearch, TableFilter.label("Estado:"), expEstado), BorderLayout.NORTH);
        expWrap.add(expCard, BorderLayout.CENTER);
        sub.addTab("Validade (expirados / a expirar)", UIHelper.icon("fas-calendar-times", 15, UIHelper.TEXT_LIGHT), expWrap);

        tab.add(sub, BorderLayout.CENTER);
        return tab;
    }

    /** Carrega os alertas: esgotados (saldo ≤ 0) e lotes expirados/a expirar em ≤ 30 dias (com stock). */
    public void refresh() {
        if (owner.alertsOutModel == null) return;
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        boolean hide = owner.stockHidden();
        UIHelper.loadAsync(owner, () -> new StockAlerts(owner.inventoryApiClient.findOutOfStockProducts(companyId),
                        owner.inventoryApiClient.findExpiringBatches(companyId, 30)),
                alerts -> applyAlerts(alerts, hide), error -> owner.showStockLoadError("alertas", error));
    }

    private void applyAlerts(StockAlerts alerts, boolean hide) {
        var esgotados = alerts.outOfStock();
        owner.alertsOutModel.setRowCount(0);
        for (var a : esgotados) {
            owner.alertsOutModel.addRow(new Object[]{
                    a.sku(), a.name(),
                    hide ? MASK : (a.currentStock() == null ? "0" : a.currentStock().toPlainString())});
        }

        var expiring = alerts.expiring();
        owner.alertsExpModel.setRowCount(0);
        LocalDate today = LocalDate.now();
        long expired = 0, soon = 0;
        for (var b : expiring) {
            long days = b.expirationDate() == null ? 0
                    : java.time.temporal.ChronoUnit.DAYS.between(today, b.expirationDate());
            boolean isExpired = days < 0;
            if (isExpired) expired++; else soon++;
            owner.alertsExpModel.addRow(new Object[]{
                    b.sku(), b.productName(), b.batchNumber(), b.warehouseName(),
                    b.expirationDate() == null ? "—" : b.expirationDate().toString(),
                    days, hide ? MASK : (b.quantity() == null ? "" : b.quantity().toPlainString()),
                    isExpired ? "Expirado" : "A expirar"});
        }

        if (owner.alertsSummary != null) {
            owner.alertsSummary.setText(hide
                    ? "Quantidades ocultas — stock trancado (visível só para administradores)."
                    : esgotados.size() + " produto(s) esgotado(s)  ·  "
                    + expired + " lote(s) expirado(s)  ·  " + soon + " a expirar (≤30 dias)");
        }
    }

    private record StockAlerts(List<StockAlertDTO> outOfStock, List<ProductBatchDTO> expiring) {}
}
