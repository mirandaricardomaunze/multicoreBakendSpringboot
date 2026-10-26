package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * Alertas de rupturas e validades do stock com alinhamento canónico ao design system.
 */
final class StockAlertsPanel {

    private final StockPanel owner;
    private JLabel outSummary;
    private JComboBox<String> expBusinessUnitCombo;
    private JComboBox<String> outBusinessUnitCombo;

    StockAlertsPanel(StockPanel owner) {
        this.owner = owner;
    }

    public JPanel buildPanel() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(10, 5, 5, 5));

        JTabbedPane sub = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(sub);

        // 1. Sub-aba: Lotes Expirados e a Expirar
        String[] expCols = {"SKU", "Nome do Artigo", "Nº Lote", "Armazém", "Validade", "Dias", "Qtd", "Estado", "Negócio"};
        owner.alertsExpModel = new DefaultTableModel(expCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        owner.alertsExpTable = new JTable(owner.alertsExpModel);
        UIHelper.styleTable(owner.alertsExpTable);
        owner.alertsExpTable.setAutoCreateRowSorter(true);

        TableCellRenderer expBase = owner.alertsExpTable.getDefaultRenderer(Object.class);
        owner.alertsExpTable.setDefaultRenderer(Object.class, StockAlertsViewCoordinator.createExpirationRenderer(owner, expBase));

        ModernPanel expCard = new ModernPanel(16);
        expCard.setLayout(new BorderLayout());
        expCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        JScrollPane expScroll = new JScrollPane(owner.alertsExpTable);
        UIHelper.styleScrollPane(expScroll);
        expCard.add(expScroll, BorderLayout.CENTER);
        expCard.add(ClientTablePagination.install(owner.alertsExpTable), BorderLayout.SOUTH);

        UIHelper.ensureHeadersFit(owner.alertsExpTable);
        JComboBox<String> expEstado = new JComboBox<>(new String[]{"Todos os estados", "Expirado", "A expirar"});
        UIHelper.styleComboBox(expEstado);
        expEstado.setPreferredSize(new Dimension(180, UIHelper.FORM_CONTROL_HEIGHT));

        expBusinessUnitCombo = TableFilter.combo("Todos os negócios");
        SearchField expSearch = new SearchField("Pesquisar por SKU, nome ou lote…");
        TableFilter.install(owner.alertsExpTable, expSearch,
                new TableFilter.ColumnFilter(expEstado, 7),
                new TableFilter.ColumnFilter(expBusinessUnitCombo, 8));

        JPanel expFilters = TableFilter.bar(
                expSearch,
                TableFilter.label("Negócio:"), expBusinessUnitCombo,
                TableFilter.label("Estado do Lote:"), expEstado
        );

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

        // 2. Sub-aba: Produtos Esgotados
        String[] outCols = {"Artigo (SKU)", "Nome do Artigo", "Stock Actual", "Negócio"};
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
        TableQuickPeekController.install(owner.alertsOutTable, outCard, (peek, mr) -> {
            int row = owner.alertsOutTable.convertRowIndexToModel(mr);
            StockAlertsViewCoordinator.populateOutOfStockPeek(peek, owner, row);
        });

        outCard.add(outScroll, BorderLayout.CENTER);
        outCard.add(ClientTablePagination.install(owner.alertsOutTable), BorderLayout.SOUTH);

        UIHelper.ensureHeadersFit(owner.alertsOutTable);
        outBusinessUnitCombo = TableFilter.combo("Todos os negócios");
        SearchField outSearch = new SearchField("Pesquisar por SKU ou nome do artigo esgotado…");
        TableFilter.install(owner.alertsOutTable, outSearch,
                new TableFilter.ColumnFilter(outBusinessUnitCombo, 3));

        JPanel outFilters = TableFilter.bar(
                outSearch,
                TableFilter.label("Negócio:"), outBusinessUnitCombo
        );

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
        UIHelper.loadAsync(owner, () -> new StockAlertsViewCoordinator.StockAlerts(
                        owner.inventoryApiClient.findOutOfStockProducts(companyId),
                        owner.inventoryApiClient.findExpiringBatches(companyId, 30)),
                alerts -> StockAlertsViewCoordinator.applyAlerts(owner, alerts, hide, outBusinessUnitCombo, expBusinessUnitCombo, outSummary),
                error -> owner.showStockLoadError("alertas", error));
    }
}
