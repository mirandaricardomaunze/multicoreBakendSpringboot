package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.inventory.dto.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/** Alertas de rupturas e validades do stock com alinhamento canónico ao design system. */
final class StockAlertsPanel {
    private static final String MASK = "•••";
    private final StockPanel owner;
    private JLabel outSummary;

    StockAlertsPanel(StockPanel owner) {
        this.owner = owner;
    }

    public JPanel buildPanel() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(10, 5, 5, 5));

        JTabbedPane sub = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(sub);

        // ==========================================
        // 1. Sub-aba: Lotes Expirados e a Expirar
        // ==========================================
        String[] expCols = {"SKU", "Nome do Artigo", "Nº Lote", "Armazém", "Validade", "Dias", "Qtd", "Estado"};
        owner.alertsExpModel = new DefaultTableModel(expCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        owner.alertsExpTable = new JTable(owner.alertsExpModel);
        UIHelper.styleTable(owner.alertsExpTable);
        owner.alertsExpTable.setAutoCreateRowSorter(true);

        javax.swing.table.TableCellRenderer expBase = owner.alertsExpTable.getDefaultRenderer(Object.class);
        owner.alertsExpTable.setDefaultRenderer(Object.class, (t, v, sel, foc, row, col) -> {
            Component c = expBase.getTableCellRendererComponent(t, v, sel, foc, row, col);
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
        expCard.add(ClientTablePagination.install(owner.alertsExpTable), BorderLayout.SOUTH);

        // Grade de filtros de Validades (Padrão GridBagLayout das outras tabelas)
        JPanel expFilters = new JPanel(new GridBagLayout());
        expFilters.setOpaque(false);
        GridBagConstraints gExp = new GridBagConstraints();
        gExp.fill = GridBagConstraints.HORIZONTAL;
        gExp.insets = new Insets(0, 0, 0, 12);

        JComboBox<String> expEstado = new JComboBox<>(new String[]{"Todos os estados", "Expirado", "A expirar"});
        UIHelper.styleComboBox(expEstado);
        expEstado.setPreferredSize(new Dimension(220, UIHelper.FORM_CONTROL_HEIGHT));

        SearchField expSearch = new SearchField("Pesquisar por SKU, nome ou lote…");
        TableFilter.install(owner.alertsExpTable, expSearch, new TableFilter.ColumnFilter(expEstado, 7));

        gExp.gridy = 0;
        gExp.gridx = 0; gExp.weightx = 0; expFilters.add(filterLabel("Estado do Lote"), gExp);
        gExp.gridx = 1; gExp.weightx = 1.0; gExp.insets = new Insets(0, 0, 0, 0);
        expFilters.add(filterLabel("Pesquisa"), gExp);

        gExp.gridy = 1; gExp.insets = new Insets(4, 0, 0, 12);
        gExp.gridx = 0; gExp.weightx = 0; expFilters.add(expEstado, gExp);
        gExp.gridx = 1; gExp.weightx = 1.0; gExp.insets = new Insets(4, 0, 0, 0);
        expFilters.add(expSearch, gExp);

        owner.alertsSummary = new JLabel("A carregar alertas de stock…");
        owner.alertsSummary.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        owner.alertsSummary.setForeground(UIHelper.TEXT_MUTED);
        owner.alertsSummary.setBorder(new EmptyBorder(8, 2, 0, 0));

        JPanel expHeaderPanel = new JPanel(new BorderLayout(0, 6));
        expHeaderPanel.setOpaque(false);
        expHeaderPanel.setBorder(new EmptyBorder(0, 0, 12, 0));
        expHeaderPanel.add(expFilters, BorderLayout.CENTER);
        expHeaderPanel.add(owner.alertsSummary, BorderLayout.SOUTH);

        ModernButton refreshExpBtn = UIHelper.createRefreshButton(this::refresh);
        expCard.add(UIHelper.tableCardTop("Lotes Expirados e a Expirar", expHeaderPanel, refreshExpBtn), BorderLayout.NORTH);
        sub.addTab("Validade (expirados / a expirar)", UIHelper.icon("fas-calendar-times", 15, UIHelper.PENDING_YELLOW), expCard);

        // ==========================================
        // 2. Sub-aba: Produtos Esgotados
        // ==========================================
        String[] outCols = {"Artigo (SKU)", "Nome do Artigo", "Stock Actual"};
        owner.alertsOutModel = new DefaultTableModel(outCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        owner.alertsOutTable = new JTable(owner.alertsOutModel);
        UIHelper.styleTable(owner.alertsOutTable);
        owner.alertsOutTable.setAutoCreateRowSorter(true);

        ModernPanel outCard = new ModernPanel(16);
        outCard.setLayout(new BorderLayout());
        outCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        JScrollPane outScroll = new JScrollPane(owner.alertsOutTable);
        UIHelper.styleScrollPane(outScroll);
        outCard.add(outScroll, BorderLayout.CENTER);
        outCard.add(ClientTablePagination.install(owner.alertsOutTable), BorderLayout.SOUTH);

        // Grade de filtros de Esgotados
        JPanel outFilters = new JPanel(new GridBagLayout());
        outFilters.setOpaque(false);
        GridBagConstraints gOut = new GridBagConstraints();
        gOut.fill = GridBagConstraints.HORIZONTAL;

        SearchField outSearch = new SearchField("Pesquisar por SKU ou nome do artigo esgotado…");
        TableFilter.install(owner.alertsOutTable, outSearch);

        gOut.gridy = 0;
        gOut.gridx = 0; gOut.weightx = 1.0;
        outFilters.add(filterLabel("Pesquisa"), gOut);

        gOut.gridy = 1;
        gOut.insets = new Insets(4, 0, 0, 0);
        gOut.gridx = 0; gOut.weightx = 1.0;
        outFilters.add(outSearch, gOut);

        outSummary = new JLabel(" ");
        outSummary.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        outSummary.setForeground(UIHelper.TEXT_MUTED);
        outSummary.setBorder(new EmptyBorder(8, 2, 0, 0));

        JPanel outHeaderPanel = new JPanel(new BorderLayout(0, 6));
        outHeaderPanel.setOpaque(false);
        outHeaderPanel.setBorder(new EmptyBorder(0, 0, 12, 0));
        outHeaderPanel.add(outFilters, BorderLayout.CENTER);
        outHeaderPanel.add(outSummary, BorderLayout.SOUTH);

        ModernButton refreshOutBtn = UIHelper.createRefreshButton(this::refresh);
        outCard.add(UIHelper.tableCardTop("Produtos Esgotados", outHeaderPanel, refreshOutBtn), BorderLayout.NORTH);
        sub.addTab("Esgotados", UIHelper.icon("fas-ban", 15, UIHelper.REJECTED_RED), outCard);

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
            if (hide) {
                owner.alertsSummary.setForeground(UIHelper.TEXT_MUTED);
                owner.alertsSummary.setText("Quantidades ocultas — stock trancado (visível só para administradores).");
            } else if (expired == 0 && soon == 0) {
                owner.alertsSummary.setForeground(UIHelper.APPROVED_GREEN);
                owner.alertsSummary.setText("Nenhum lote expirado ou a expirar nos próximos 30 dias.");
            } else {
                owner.alertsSummary.setForeground(expired > 0 ? UIHelper.REJECTED_RED : UIHelper.PENDING_YELLOW);
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

    private JLabel filterLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UIHelper.TEXT_MUTED);
        label.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        return label;
    }

    private record StockAlerts(List<StockAlertDTO> outOfStock, List<ProductBatchDTO> expiring) {}
}
