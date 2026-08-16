package com.phcpro.gui;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.gui.components.*;
import com.phcpro.modules.purchases.dto.ReorderSuggestionDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Sugestões de reposição calculadas pelo backend. */
final class PurchaseReorderPanel {
    private final ComprasPanel owner;
    private JTable reorderTable;
    private JLabel reorderFooter;
    PurchaseReorderPanel(ComprasPanel owner) { this.owner = owner; }

    public JPanel buildPanel() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(12, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false);
        header.add(UIHelper.createHeading("Reposição Automática (abaixo do mínimo)"), BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); actions.setOpaque(false);
        ModernButton orderBtn = UIHelper.createSuccessButton("Criar Encomenda");
        orderBtn.setIcon(UIHelper.icon("fas-clipboard-list", 14));
        orderBtn.setToolTipText("Abre a aba Encomendas a Fornecedor para encomendar os produtos em falta.");
        orderBtn.addActionListener(e -> owner.tabbedPane.setSelectedIndex(2)); // Encomendas a Fornecedor
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Actualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshBtn.addActionListener(e -> refresh());
        actions.add(refreshBtn); actions.add(orderBtn);
        header.add(actions, BorderLayout.EAST);
        tab.add(header, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        String[] cols = {"Produto", "SKU", "Stock Atual", "Mínimo", "Und/Caixa", "Sugerido (caixas)", "Sugerido (unidades)", "Estado"};
        owner.reorderModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        reorderTable = new JTable(owner.reorderModel);
        UIHelper.styleTable(reorderTable);
        reorderTable.getColumnModel().getColumn(7).setCellRenderer(TableCellRenderers.status());
        JScrollPane scroll = new JScrollPane(reorderTable);
        UIHelper.styleScrollPane(scroll);

        JTextField reorderSearch = TableFilter.searchField("Produto ou SKU…");
        JComboBox<String> reorderEstado = TableFilter.combo("Todos os estados", "ESGOTADO", "BAIXO");
        TableFilter.install(reorderTable, reorderSearch,
                new TableFilter.ColumnFilter(reorderEstado, 7));
        JPanel reorderBar = TableFilter.bar(reorderSearch,
                TableFilter.label("Estado:"), reorderEstado);
        reorderBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(reorderBar, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        reorderFooter = new JLabel(" ");
        reorderFooter.setForeground(UIHelper.TEXT_MUTED);
        reorderFooter.setBorder(new EmptyBorder(8, 4, 0, 4));
        card.add(reorderFooter, BorderLayout.SOUTH);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    public void refresh() {
        if (owner.reorderModel == null) return;
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(owner, () -> owner.purchaseApiClient.suggestions(companyId), this::applyReorderSuggestions,
                error -> owner.showPurchaseLoadError("sugestões de reposição", error));
    }

    private void applyReorderSuggestions(java.util.List<com.phcpro.modules.purchases.dto.ReorderSuggestionDTO> loaded) {
        owner.reorderList = loaded;
        owner.reorderModel.setRowCount(0);
        for (var s : owner.reorderList) {
            String estado = s.currentStock().signum() <= 0 ? "ESGOTADO" : "BAIXO";
            owner.reorderModel.addRow(new Object[]{
                    s.name(), s.sku(),
                    String.format("%,.3f", s.currentStock()),
                    String.format("%,.3f", s.minStock()),
                    s.unitsPerBox(),
                    String.format("%,.0f", s.suggestedBoxes()),
                    String.format("%,.0f", s.suggestedUnits()),
                    estado});
        }
        reorderFooter.setText(owner.reorderList.isEmpty()
                ? "Sem reposições pendentes — todo o stock está acima do mínimo."
                : String.format("%d produto(s) a repor.", owner.reorderList.size()));
    }

    // ===== Contas a Pagar =====

}
