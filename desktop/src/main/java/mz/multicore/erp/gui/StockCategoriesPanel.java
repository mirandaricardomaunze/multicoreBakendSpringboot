package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Vista, pesquisa e manutenção de categorias de produto.
 */
final class StockCategoriesPanel {

    private final StockPanel owner;

    StockCategoriesPanel(StockPanel owner) {
        this.owner = owner;
    }

    public JPanel buildPanel() {
        JPanel tab = new JPanel(new BorderLayout(0, 8));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(4, 0, 0, 0));

        ModernButton newBtn = UIHelper.createSuccessButton("Nova Categoria");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newBtn.addActionListener(e -> StockCategoryFormDialog.open(owner, null, this::refresh));

        ModernButton editBtn = UIHelper.createSecondaryButton("Editar");
        editBtn.setIcon(UIHelper.icon("fas-edit", 14));
        editBtn.addActionListener(e -> {
            var sel = StockCategoriesViewCoordinator.selectedCategory(owner);
            if (sel != null) StockCategoryFormDialog.open(owner, sel, this::refresh);
        });

        ModernButton refreshBtn = UIHelper.createRefreshButton(this::refresh);
        ActionMenuButton moreBtn = UIHelper.createActionMenuButton("Mais acções")
                .addAction("Activar/Desactivar", UIHelper.icon("fas-power-off", 14), () -> StockCategoriesViewCoordinator.toggleSelectedCategory(owner, this::refresh));

        owner.categorySearchField = TableFilter.searchField("Pesquisar categoria por código ou nome…");
        UIHelper.onTextChange(owner.categorySearchField, () -> StockCategoriesViewCoordinator.filterCategories(owner, owner.categorySearchField.getText()));
        JPanel searchRow = TableFilter.bar(owner.categorySearchField);
        searchRow.setBorder(new EmptyBorder(0, 0, 4, 0));

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(8, 12, 8, 12));
        card.add(UIHelper.tableCardTop("Categorias de Produto", searchRow,
                refreshBtn, moreBtn, editBtn, newBtn), BorderLayout.NORTH);

        String[] cols = {"Código", "Nome", "Cor", "Produtos", "Estado"};
        owner.categoriesModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        owner.categoriesTable = new JTable(owner.categoriesModel);
        UIHelper.styleTable(owner.categoriesTable);
        owner.categoriesTable.setAutoCreateRowSorter(true);
        owner.categoriesTable.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.status());
        owner.categoriesTable.getColumnModel().getColumn(2).setCellRenderer(StockCategoriesViewCoordinator.createColorCellRenderer());
        owner.categoriesTable.getColumnModel().getColumn(3).setMaxWidth(90);

        UIHelper.ensureHeadersFit(owner.categoriesTable);
        JScrollPane scroll = new JScrollPane(owner.categoriesTable);
        UIHelper.styleScrollPane(scroll);

        TableQuickPeekController.install(owner.categoriesTable, card, (peek, mr) -> {
            int row = mr; // o controlador já entrega a linha do modelo
            StockCategoriesViewCoordinator.populateQuickPeek(peek, owner, row);
        });

        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(owner.categoriesTable), BorderLayout.SOUTH);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    public void refresh() {
        if (owner.categoriesModel == null) return;
        UIHelper.loadAsync(owner, () -> new StockCategoriesViewCoordinator.CategoryData(
                        owner.productCategoryApiClient.getAll(),
                        owner.comercialApiClient.getAllProducts()),
                data -> StockCategoriesViewCoordinator.applyCategories(owner, data),
                error -> owner.showStockLoadError("categorias", error));
    }
}
