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
import java.util.List;
import java.util.Locale;

/**
 * Painel analítico de Reposição Inteligente de Stock:
 * Apresenta previsão de rutura em dias, velocidade de rotação diária (últimos 30 dias),
 * fornecedor habitual, custo estimado e geração de encomendas a fornecedor em 1 clique.
 */
final class PurchaseReorderPanel {

    private final ComprasPanel owner;
    private JTable reorderTable;
    private JLabel reorderFooter;

    // KPI Card Value Labels
    private final JLabel kpiEsgotados = new JLabel("0");
    private final JLabel kpiCriticos = new JLabel("0");
    private final JLabel kpiTotalRepor = new JLabel("0");
    private final JLabel kpiCustoTotal = new JLabel("0,00 MT");

    private JComboBox<String> supplierFilterCombo;

    PurchaseReorderPanel(ComprasPanel owner) {
        this.owner = owner;
    }

    public JPanel buildPanel() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(12, 5, 5, 5));

        // 1. Cabeçalho com Título, Subtítulo e Ações
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBox.setOpaque(false);
        titleBox.add(UIHelper.createHeading("Reposição Inteligente & Previsão de Rutura"));
        JLabel subtitle = new JLabel("Previsão automática de rutura com base nas vendas dos últimos 30 dias e sugestão a caixas inteiras.");
        subtitle.setForeground(UIHelper.TEXT_MUTED);
        subtitle.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        titleBox.add(subtitle);
        header.add(titleBox, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        ModernButton refreshBtn = UIHelper.createRefreshButton(this::refresh);

        ModernButton orderBtn = UIHelper.createSuccessButton("Criar Encomenda");
        orderBtn.setIcon(UIHelper.icon("fas-cart-plus", 14));
        orderBtn.setToolTipText("Cria encomenda preenchida com o produto selecionado ou abre nova encomenda.");
        orderBtn.addActionListener(e -> orderSelectedOrOpen());

        actions.add(refreshBtn);
        actions.add(orderBtn);
        header.add(actions, BorderLayout.EAST);

        // 2. Banner com 4 KPI Cards
        JPanel kpiGrid = new JPanel(new GridLayout(1, 4, 10, 0));
        kpiGrid.setOpaque(false);
        kpiGrid.add(createKpiCard("Produtos Esgotados", kpiEsgotados, "fas-exclamation-triangle", UIHelper.REJECTED_RED));
        kpiGrid.add(createKpiCard("Ruptura Iminente (≤ 7d)", kpiCriticos, "fas-fire", UIHelper.PENDING_YELLOW));
        kpiGrid.add(createKpiCard("Total a Repor", kpiTotalRepor, "fas-boxes", UIHelper.ACCENT_BLUE));
        kpiGrid.add(createKpiCard("Custo Estimado", kpiCustoTotal, "fas-coins", UIHelper.APPROVED_GREEN));

        JPanel northPanel = new JPanel(new BorderLayout(0, 10));
        northPanel.setOpaque(false);
        northPanel.add(header, BorderLayout.NORTH);
        northPanel.add(kpiGrid, BorderLayout.SOUTH);
        tab.add(northPanel, BorderLayout.NORTH);

        // 3. Card Principal com Barra de Filtro e Tabela
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

        // Renderers especializados
        reorderTable.getColumnModel().getColumn(9).setPreferredWidth(140);
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

        // Barra de Filtros
        JTextField reorderSearch = TableFilter.searchField("Pesquisar produto, SKU ou fornecedor…");
        JComboBox<String> reorderUrgencia = TableFilter.combo("Todas as urgências", "ESGOTADO", "CRÍTICO", "BAIXO");
        UIHelper.styleComboBox(reorderUrgencia);
        reorderUrgencia.setPreferredSize(new Dimension(180, UIHelper.FORM_CONTROL_HEIGHT));

        supplierFilterCombo = TableFilter.combo("Todos os fornecedores");
        UIHelper.styleComboBox(supplierFilterCombo);
        supplierFilterCombo.setPreferredSize(new Dimension(220, UIHelper.FORM_CONTROL_HEIGHT));

        TableFilter.install(reorderTable, reorderSearch,
                new TableFilter.ColumnFilter(reorderUrgencia, 12),
                new TableFilter.ColumnFilter(supplierFilterCombo, 9));

        JPanel reorderFilters = new JPanel(new GridBagLayout());
        reorderFilters.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, 0, 12);

        g.gridx = 0; g.weightx = 0; reorderFilters.add(filterLabel("Urgência"), g);
        g.gridx = 1; g.weightx = 0; reorderFilters.add(filterLabel("Fornecedor"), g);
        g.gridx = 2; g.weightx = 1.0; g.insets = new Insets(0, 0, 0, 0);
        reorderFilters.add(filterLabel("Pesquisa"), g);

        g.gridy = 1;
        g.insets = new Insets(4, 0, 0, 12);
        g.gridx = 0; g.weightx = 0; reorderFilters.add(reorderUrgencia, g);
        g.gridx = 1; g.weightx = 0; reorderFilters.add(supplierFilterCombo, g);
        g.gridx = 2; g.weightx = 1.0; g.insets = new Insets(4, 0, 0, 0);
        reorderFilters.add(reorderSearch, g);

        reorderFilters.setBorder(new EmptyBorder(0, 0, 6, 0));
        card.add(UIHelper.tableCardTop("Reposição Inteligente & Previsão de Rutura", reorderFilters,
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

        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private ModernPanel createKpiCard(String title, JLabel valLabel, String icon, Color accent) {
        ModernPanel card = new ModernPanel(10);
        card.setLayout(new BorderLayout(0, 4));
        card.setBorder(new EmptyBorder(8, 12, 8, 12));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(titleLbl.getFont().deriveFont(Font.PLAIN, 11f));
        titleLbl.setForeground(UIHelper.TEXT_MUTED);
        top.add(titleLbl, BorderLayout.WEST);

        JLabel iconLbl = new JLabel(UIHelper.icon(icon, 14, accent));
        top.add(iconLbl, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        valLabel.setFont(valLabel.getFont().deriveFont(Font.BOLD, 15f));
        valLabel.setForeground(accent);
        card.add(valLabel, BorderLayout.CENTER);

        return card;
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
    private JLabel filterLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(UIHelper.TEXT_MUTED);
        return label;
    }
}
