package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Painel analítico de Reposição Inteligente de Stock:
 * Apresenta previsão de rutura em dias, velocidade de rotação diária (últimos 30 dias),
 * fornecedor habitual, custo estimado e geração de encomendas a fornecedor em 1 clique.
 * Segue o padrão canónico do sistema: KPIs {@link KpiCard} interactivos, card único
 * {@link ModernPanel}(16) com título, acções e filtros no topo, e «Quick Peek» por Espaço.
 */
final class PurchaseReorderPanel {

    private static final String ALL_URGENCIES = "Todas as urgências";

    private final ComprasPanel owner;
    private JTable reorderTable;
    private JLabel reorderFooter;

    private final JLabel kpiEsgotados = new JLabel("0");
    private final JLabel kpiCriticos = new JLabel("0");
    private final JLabel kpiTotalRepor = new JLabel("0");
    private final JLabel kpiCustoTotal = new JLabel("0,00 MT");

    JComboBox<String> urgencyFilterCombo;
    JComboBox<String> supplierFilterCombo;
    JComboBox<String> businessUnitFilterCombo;

    PurchaseReorderPanel(ComprasPanel owner) {
        this.owner = owner;
    }

    public JPanel buildPanel() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(12, 5, 5, 5));

        JPanel kpiGrid = KpiCard.createGrid(4);
        kpiGrid.add(KpiCard.createInteractiveCard("Produtos Esgotados", kpiEsgotados, "Sem stock disponível",
                "fas-exclamation-triangle", UIHelper.REJECTED_RED,
                "Filtrar produtos esgotados", () -> filterUrgency("ESGOTADO")));
        kpiGrid.add(KpiCard.createInteractiveCard("Ruptura Iminente", kpiCriticos, "Cobertura até 7 dias",
                "fas-fire", UIHelper.PENDING_YELLOW,
                "Filtrar produtos em ruptura iminente", () -> filterUrgency("CRÍTICO")));
        kpiGrid.add(KpiCard.createInteractiveCard("Total a Repor", kpiTotalRepor, "Produtos com sugestão",
                "fas-boxes", UIHelper.ACCENT_BLUE,
                "Mostrar todos os produtos a repor", () -> filterUrgency(null)));
        kpiGrid.add(KpiCard.createCard("Custo Estimado", kpiCustoTotal, "Investimento sugerido",
                "fas-coins", UIHelper.APPROVED_GREEN));
        tab.add(kpiGrid, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        String[] cols = {
                "Produto", "SKU", "Stock Atual", "Mínimo", "Venda/Dia",
                "Dias Stock", "Und/Cx", "Sugerido (cx)", "Sugerido (und)",
                "Fornecedor Habitual", "Preço Unit.", "Custo Total", "Urgência", "Negócio"
        };
        owner.reorderModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        reorderTable = new JTable(owner.reorderModel);
        UIHelper.styleTable(reorderTable);
        reorderTable.putClientProperty("noRowInspector", Boolean.TRUE);

        int[] widths = {200, 90, 90, 80, 100, 90, 70, 100, 110, 160, 100, 110, 100, 110};
        for (int i = 0; i < widths.length; i++) {
            reorderTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        UIHelper.ensureHeadersFit(reorderTable);

        reorderTable.getColumnModel().getColumn(10).setCellRenderer(TableCellRenderers.money());
        reorderTable.getColumnModel().getColumn(11).setCellRenderer(TableCellRenderers.money());
        reorderTable.getColumnModel().getColumn(12).setCellRenderer(TableCellRenderers.status());

        reorderTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && reorderTable.getSelectedRow() >= 0) {
                    orderSelectedOrOpen();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(reorderTable);
        UIHelper.styleScrollPane(scroll);

        JTextField reorderSearch = TableFilter.searchField("Pesquisar produto, SKU ou fornecedor…");
        reorderSearch.setPreferredSize(new Dimension(160, UIHelper.FORM_CONTROL_HEIGHT));
        businessUnitFilterCombo = TableFilter.combo("Todos os negócios");
        businessUnitFilterCombo.setPreferredSize(new Dimension(140, UIHelper.FORM_CONTROL_HEIGHT));
        urgencyFilterCombo = TableFilter.combo(ALL_URGENCIES, "ESGOTADO", "CRÍTICO", "BAIXO");
        urgencyFilterCombo.setPreferredSize(new Dimension(130, UIHelper.FORM_CONTROL_HEIGHT));
        supplierFilterCombo = TableFilter.combo("Todos os fornecedores");
        supplierFilterCombo.setPreferredSize(new Dimension(145, UIHelper.FORM_CONTROL_HEIGHT));

        TableFilter.install(reorderTable, reorderSearch,
                new TableFilter.ColumnFilter(businessUnitFilterCombo, 13),
                new TableFilter.ColumnFilter(urgencyFilterCombo, 12),
                new TableFilter.ColumnFilter(supplierFilterCombo, 9));

        JPanel reorderFilters = TableFilter.bar(
                reorderSearch,
                TableFilter.label("Negócio:"), businessUnitFilterCombo,
                TableFilter.label("Urgência:"), urgencyFilterCombo,
                TableFilter.label("Fornecedor:"), supplierFilterCombo);

        ModernButton peekToggleBtn = UIHelper.createSecondaryButton("");
        peekToggleBtn.setIcon(UIHelper.icon("fas-columns", 12));
        peekToggleBtn.setToolTipText("Espreitar detalhes da linha seleccionada (Espaço)");
        peekToggleBtn.getAccessibleContext().setAccessibleName("Espreitar detalhes da linha");
        peekToggleBtn.setPreferredSize(new Dimension(30, UIHelper.FORM_CONTROL_HEIGHT));

        JPanel filtersRow = new JPanel(new BorderLayout(8, 0));
        filtersRow.setOpaque(false);
        filtersRow.setBorder(new EmptyBorder(0, 0, 6, 0));
        filtersRow.add(reorderFilters, BorderLayout.WEST);
        filtersRow.add(peekToggleBtn, BorderLayout.EAST);

        ModernButton refreshBtn = UIHelper.createRefreshButton(this::refresh);

        ModernButton orderBtn = UIHelper.createSuccessButton("Criar Encomenda");
        orderBtn.setIcon(UIHelper.icon("fas-cart-plus", 14));
        orderBtn.setToolTipText("Cria encomenda preenchida com o produto selecionado ou abre nova encomenda.");
        orderBtn.addActionListener(e -> orderSelectedOrOpen());

        card.add(UIHelper.tableCardTop("Reposição Inteligente & Previsão de Rutura", filtersRow,
                refreshBtn, orderBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        reorderFooter = new JLabel(" ");
        reorderFooter.setForeground(UIHelper.TEXT_MUTED);
        reorderFooter.setBorder(new EmptyBorder(6, 4, 0, 4));
        JPanel reorderSouth = new JPanel(new BorderLayout());
        reorderSouth.setOpaque(false);
        reorderSouth.add(ClientTablePagination.install(reorderTable), BorderLayout.NORTH);
        reorderSouth.add(reorderFooter, BorderLayout.SOUTH);
        card.add(reorderSouth, BorderLayout.SOUTH);

        TableQuickPeekController peek = TableQuickPeekController.install(reorderTable, card,
                (p, mr) -> PurchaseReorderOperationsCoordinator.populatePeek(p, owner.reorderList, mr, this::orderSelectedOrOpen));
        peekToggleBtn.addActionListener(e -> peek.toggle());

        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void filterUrgency(String urgency) {
        if (urgencyFilterCombo == null) return;
        if (urgency == null) {
            urgencyFilterCombo.setSelectedIndex(0);
        } else {
            urgencyFilterCombo.setSelectedItem(urgency);
        }
    }

    public void refresh() {
        if (owner.reorderModel == null) return;
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(owner, () -> owner.purchaseApiClient.suggestions(companyId),
                loaded -> PurchaseReorderDataCoordinator.applyReorderSuggestions(
                        loaded, owner, businessUnitFilterCombo, supplierFilterCombo,
                        kpiEsgotados, kpiCriticos, kpiTotalRepor, kpiCustoTotal, reorderFooter
                ),
                error -> owner.showPurchaseLoadError("sugestões de reposição", error));
    }

    private void orderSelectedOrOpen() {
        PurchaseReorderOperationsCoordinator.orderSelectedOrOpen(reorderTable, owner.reorderList, owner.purchaseOrdersPanel);
    }
}
