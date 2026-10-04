package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.purchases.dto.ReorderSuggestionDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    // KPI Card Value Labels
    private final JLabel kpiEsgotados = new JLabel("0");
    private final JLabel kpiCriticos = new JLabel("0");
    private final JLabel kpiTotalRepor = new JLabel("0");
    private final JLabel kpiCustoTotal = new JLabel("0,00 MT");

    JComboBox<String> urgencyFilterCombo;
    JComboBox<String> supplierFilterCombo;

    PurchaseReorderPanel(ComprasPanel owner) {
        this.owner = owner;
    }

    public JPanel buildPanel() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(12, 5, 5, 5));

        // 1. KPIs canónicos (altura uniforme) com drilldown por urgência
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

        // 2. Card principal único: título + acções + filtros + tabela
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        String[] cols = {
                "Produto", "SKU", "Stock Atual", "Mínimo", "Venda/Dia",
                "Dias Stock", "Und/Cx", "Sugerido (cx)", "Sugerido (und)",
                "Fornecedor Habitual", "Preço Unit.", "Custo Total", "Urgência"
        };
        owner.reorderModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        reorderTable = new JTable(owner.reorderModel);
        UIHelper.styleTable(reorderTable);
        reorderTable.putClientProperty("noRowInspector", Boolean.TRUE);

        int[] widths = {200, 90, 90, 80, 100, 90, 70, 100, 110, 160, 100, 110, 100};
        for (int i = 0; i < widths.length; i++) {
            reorderTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        UIHelper.ensureHeadersFit(reorderTable);

        // Renderers especializados
        reorderTable.getColumnModel().getColumn(10).setCellRenderer(TableCellRenderers.money());
        reorderTable.getColumnModel().getColumn(11).setCellRenderer(TableCellRenderers.money());
        reorderTable.getColumnModel().getColumn(12).setCellRenderer(TableCellRenderers.status());

        // Duplo clique na linha para abrir encomenda pré-preenchida
        reorderTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && reorderTable.getSelectedRow() >= 0) {
                    orderSelectedOrOpen();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(reorderTable);
        UIHelper.styleScrollPane(scroll);

        // Barra de filtros numa única linha horizontal
        JTextField reorderSearch = TableFilter.searchField("Pesquisar produto, SKU ou fornecedor…");
        urgencyFilterCombo = TableFilter.combo(ALL_URGENCIES, "ESGOTADO", "CRÍTICO", "BAIXO");
        supplierFilterCombo = TableFilter.combo("Todos os fornecedores");

        TableFilter.install(reorderTable, reorderSearch,
                new TableFilter.ColumnFilter(urgencyFilterCombo, 12),
                new TableFilter.ColumnFilter(supplierFilterCombo, 9));

        JPanel reorderFilters = TableFilter.bar(
                reorderSearch,
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

        // 3. Quick Peek silencioso (tecla Espaço)
        TableQuickPeekController peek = TableQuickPeekController.install(reorderTable, card, this::populatePeek);
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

    private void populatePeek(QuickPeekPanel peek, int modelRow) {
        if (owner.reorderList == null || modelRow < 0 || modelRow >= owner.reorderList.size()) return;
        ReorderSuggestionDTO s = owner.reorderList.get(modelRow);
        String status = s.urgencyStatus() != null ? s.urgencyStatus()
                : (s.currentStock().signum() <= 0 ? "ESGOTADO" : "BAIXO");
        Color statusColor = "ESGOTADO".equalsIgnoreCase(status) ? UIHelper.REJECTED_RED
                : (status.toUpperCase(Locale.ROOT).startsWith("CR") ? UIHelper.PENDING_YELLOW : UIHelper.ACCENT_BLUE);

        peek.setHeaderIcon("fas-cart-plus", UIHelper.ACCENT_BLUE);
        peek.setTitle(s.name());
        peek.setSubtitle("SKU " + (s.sku() != null ? s.sku() : "—"));
        peek.setStatus(status, statusColor);

        String supplier = (s.supplierName() != null && !s.supplierName().isBlank()) ? s.supplierName() : "—";
        BigDecimal rate = s.dailySalesRate() != null ? s.dailySalesRate() : BigDecimal.ZERO;
        BigDecimal unitPrice = s.estimatedUnitPrice() != null ? s.estimatedUnitPrice() : BigDecimal.ZERO;
        BigDecimal totalCost = s.estimatedTotalCost() != null ? s.estimatedTotalCost() : BigDecimal.ZERO;

        List<QuickPeekPanel.PeekItem> items = new ArrayList<>();
        items.add(new QuickPeekPanel.PeekItem("Fornecedor habitual", supplier, false));
        items.add(new QuickPeekPanel.PeekItem("Stock actual / mínimo",
                String.format(Locale.ROOT, "%,.2f / %,.2f", s.currentStock(), s.minStock()), false));
        items.add(new QuickPeekPanel.PeekItem("Venda média diária",
                String.format(Locale.ROOT, "%,.2f /dia", rate), false));
        items.add(new QuickPeekPanel.PeekItem("Cobertura restante",
                s.daysRemaining() != null ? s.daysRemaining() + (s.daysRemaining() == 1 ? " dia" : " dias") : "—", false));
        items.add(new QuickPeekPanel.PeekItem("Embalagem",
                s.unitsPerBox() + " und/cx — sugerido " + String.format(Locale.ROOT, "%,.0f cx (%,.0f und)",
                        s.suggestedBoxes(), s.suggestedUnits()), false));
        items.add(new QuickPeekPanel.PeekItem("Preço unitário estimado",
                String.format(Locale.ROOT, "%,.2f MT", unitPrice), false));
        items.add(new QuickPeekPanel.PeekItem("Custo total previsto",
                String.format(Locale.ROOT, "%,.2f MT", totalCost), true));
        peek.setItems(items);
        peek.setOnOpenFullAction(ignored -> orderSelectedOrOpen());
    }

    public void refresh() {
        if (owner.reorderModel == null) return;
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(owner, () -> owner.purchaseApiClient.suggestions(companyId), this::applyReorderSuggestions,
                error -> owner.showPurchaseLoadError("sugestões de reposição", error));
    }

    private void applyReorderSuggestions(List<ReorderSuggestionDTO> loaded) {
        owner.reorderList = loaded != null ? loaded : List.of();
        owner.reorderModel.setRowCount(0);

        int esgotadosCount = 0;
        int criticosCount = 0;
        BigDecimal totalCostSum = BigDecimal.ZERO;
        java.util.Set<String> uniqueSuppliers = new java.util.TreeSet<>();

        for (ReorderSuggestionDTO s : owner.reorderList) {
            String status = s.urgencyStatus() != null ? s.urgencyStatus() : (s.currentStock().signum() <= 0 ? "ESGOTADO" : "BAIXO");
            if ("ESGOTADO".equalsIgnoreCase(status)) {
                esgotadosCount++;
            } else if ("CRÍTICO".equalsIgnoreCase(status) || "CRITICO".equalsIgnoreCase(status)) {
                criticosCount++;
            }

            BigDecimal estCost = s.estimatedTotalCost() != null ? s.estimatedTotalCost() : BigDecimal.ZERO;
            totalCostSum = totalCostSum.add(estCost);

            String supName = (s.supplierName() != null && !s.supplierName().isBlank()) ? s.supplierName() : "—";
            if (!"—".equals(supName)) {
                uniqueSuppliers.add(supName);
            }

            String daysStr = s.daysRemaining() != null ? (s.daysRemaining() + (s.daysRemaining() == 1 ? " dia" : " dias")) : "—";
            BigDecimal salesRate = s.dailySalesRate() != null ? s.dailySalesRate() : BigDecimal.ZERO;

            owner.reorderModel.addRow(new Object[]{
                    s.name(),
                    s.sku(),
                    String.format(Locale.ROOT, "%,.2f", s.currentStock()),
                    String.format(Locale.ROOT, "%,.2f", s.minStock()),
                    String.format(Locale.ROOT, "%,.2f /dia", salesRate),
                    daysStr,
                    s.unitsPerBox(),
                    String.format(Locale.ROOT, "%,.0f", s.suggestedBoxes()),
                    String.format(Locale.ROOT, "%,.0f", s.suggestedUnits()),
                    supName,
                    s.estimatedUnitPrice() != null ? s.estimatedUnitPrice() : BigDecimal.ZERO,
                    estCost,
                    status
            });
        }

        // Actualizar KPI Cards
        kpiEsgotados.setText(String.valueOf(esgotadosCount));
        kpiCriticos.setText(String.valueOf(criticosCount));
        kpiTotalRepor.setText(String.valueOf(owner.reorderList.size()));
        NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("pt", "MZ"));
        fmt.setMinimumFractionDigits(2);
        fmt.setMaximumFractionDigits(2);
        kpiCustoTotal.setText(fmt.format(totalCostSum) + " MT");

        // Actualizar combo de fornecedores mantendo o item 0 ("Todos os fornecedores")
        if (supplierFilterCombo != null) {
            String prevSel = (String) supplierFilterCombo.getSelectedItem();
            supplierFilterCombo.removeAllItems();
            supplierFilterCombo.addItem("Todos os fornecedores");
            for (String sup : uniqueSuppliers) {
                supplierFilterCombo.addItem(sup);
            }
            if (prevSel != null && uniqueSuppliers.contains(prevSel)) {
                supplierFilterCombo.setSelectedItem(prevSel);
            }
        }

        // Actualizar rodapé
        reorderFooter.setText(owner.reorderList.isEmpty()
                ? "Sem reposições pendentes — todo o stock está em níveis adequados acima do mínimo."
                : String.format("%d produto(s) a repor · %d esgotado(s) · %d crítico(s) · Investimento estimado: %s MT",
                owner.reorderList.size(), esgotadosCount, criticosCount, fmt.format(totalCostSum)));
    }

    private void orderSelectedOrOpen() {
        int viewRow = reorderTable != null ? reorderTable.getSelectedRow() : -1;
        if (viewRow >= 0) {
            int modelRow = reorderTable.convertRowIndexToModel(viewRow);
            if (modelRow >= 0 && modelRow < owner.reorderList.size()) {
                ReorderSuggestionDTO sel = owner.reorderList.get(modelRow);
                owner.purchaseOrdersPanel.openPurchaseOrderForSuggestion(
                        sel.supplierId(),
                        sel.productId(),
                        sel.suggestedUnits(),
                        sel.estimatedUnitPrice()
                );
                return;
            }
        }
        // Se nenhuma linha selecionada, abre o diálogo de nova encomenda normal
        owner.purchaseOrdersPanel.openPurchaseOrderFormDialog();
    }
}
