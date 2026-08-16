package com.phcpro.gui;

import com.phcpro.gui.components.*;
import com.phcpro.modules.comercial.dto.ProductCategoryDTO;
import com.phcpro.modules.comercial.dto.ProductDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/** Vista, pesquisa e manutenção de categorias de produto. */
final class StockCategoriesPanel {
    private final StockPanel owner;
    StockCategoriesPanel(StockPanel owner) { this.owner = owner; }

    public JPanel buildPanel() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(12, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false);
        header.add(UIHelper.createHeading("Categorias de Produto"), BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); actions.setOpaque(false);
        ModernButton newBtn = UIHelper.createSuccessButton("Nova Categoria");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newBtn.addActionListener(e -> openCategoryDialog(null));
        ModernButton editBtn = UIHelper.createSecondaryButton("Editar");
        editBtn.setIcon(UIHelper.icon("fas-edit", 14));
        editBtn.addActionListener(e -> {
            var sel = selectedCategory();
            if (sel != null) openCategoryDialog(sel);
        });
        ActionMenuButton moreBtn = UIHelper.createActionMenuButton("Mais acções")
                .addAction("Activar/Desactivar", UIHelper.icon("fas-power-off", 14), this::toggleSelectedCategory)
                .addAction("Actualizar", UIHelper.icon("fas-sync-alt", 14), this::refresh);
        actions.add(moreBtn); actions.add(editBtn); actions.add(newBtn);
        header.add(actions, BorderLayout.EAST);

        // Pesquisa por código/nome
        owner.categorySearchField = new SearchField("Pesquisar categoria por código ou nome…");
        UIHelper.onTextChange(owner.categorySearchField, () -> filterCategories(owner.categorySearchField.getText()));
        JPanel searchRow = new JPanel(new BorderLayout());
        searchRow.setOpaque(false);
        searchRow.setBorder(new EmptyBorder(10, 0, 0, 0));
        searchRow.add(owner.categorySearchField, BorderLayout.CENTER);

        JPanel headerWrap = new JPanel(new BorderLayout());
        headerWrap.setOpaque(false);
        headerWrap.add(header, BorderLayout.NORTH);
        headerWrap.add(searchRow, BorderLayout.SOUTH);
        tab.add(headerWrap, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        String[] cols = {"Código", "Nome", "Cor", "Produtos", "Estado"};
        owner.categoriesModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        owner.categoriesTable = new JTable(owner.categoriesModel);
        UIHelper.styleTable(owner.categoriesTable);
        owner.categoriesTable.setAutoCreateRowSorter(true);
        owner.categoriesTable.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.status());
        // Coluna "Cor" com amostra visível da cor da categoria
        owner.categoriesTable.getColumnModel().getColumn(2).setCellRenderer(new ColorCellRenderer());
        owner.categoriesTable.getColumnModel().getColumn(3).setMaxWidth(90);
        JScrollPane scroll = new JScrollPane(owner.categoriesTable);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);
        card.add(com.phcpro.gui.components.ClientTablePagination.install(owner.categoriesTable), BorderLayout.SOUTH);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    public void refresh() {
        if (owner.categoriesModel == null) return;
        UIHelper.loadAsync(owner, () -> new CategoryData(owner.productCategoryApiClient.getAll(),
                        owner.comercialApiClient.getAllProducts()), this::applyCategories,
                error -> owner.showStockLoadError("categorias", error));
    }

    private void applyCategories(CategoryData data) {
        owner.categoriesList = data.categories();
        owner.catalogProducts = data.products();
        // Contagem de produtos por categoria (gestão profissional: saber o que está em uso)
        owner.categoryProductCounts.clear();
        for (ProductDTO p : data.products()) {
            if (p.categoryId() != null) {
                owner.categoryProductCounts.merge(p.categoryId(), 1, Integer::sum);
            }
        }
        filterCategories(owner.categorySearchField == null ? "" : owner.categorySearchField.getText());
    }

    private void filterCategories(String query) {
        if (owner.categoriesModel == null) return;
        String q = query == null ? "" : query.trim().toLowerCase();
        owner.categoriesFiltered = owner.categoriesList.stream()
                .filter(c -> q.isEmpty()
                        || (c.code() != null && c.code().toLowerCase().contains(q))
                        || (c.name() != null && c.name().toLowerCase().contains(q)))
                .toList();
        owner.categoriesModel.setRowCount(0);
        for (var c : owner.categoriesFiltered) {
            owner.categoriesModel.addRow(new Object[]{
                    c.code(), c.name(),
                    c.colorHex() != null && !c.colorHex().isBlank() ? c.colorHex() : "—",
                    owner.categoryProductCounts.getOrDefault(c.id(), 0),
                    c.active() ? "Activa" : "Inactiva"});
        }
    }

    private com.phcpro.modules.comercial.dto.ProductCategoryDTO selectedCategory() {
        int row = owner.categoriesTable.getSelectedRow();
        if (row < 0 || row >= owner.categoriesFiltered.size()) {
            JOptionPane.showMessageDialog(owner, "Selecione uma categoria.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return owner.categoriesFiltered.get(row);
    }

    /** Renderiza a coluna "Cor" com uma amostra (quadrado) da cor hex da categoria + o código hex. */
    private static class ColorCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int col) {
            super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
            setBackground(isSelected ? t.getSelectionBackground() : (row % 2 == 0 ? UIHelper.BG_CARD : UIHelper.ROW_ALT));
            setForeground(UIHelper.TEXT_LIGHT);
            String hex = value == null ? "" : value.toString();
            setIcon(colorSwatchIcon(hex, 14));
            setText("  " + (hex.isBlank() ? "—" : hex));
            setBorder(new EmptyBorder(0, 10, 0, 10));
            return this;
        }
    }

    /** Ícone quadrado preenchido com a cor hex (ou contorno cinza quando inválida/ausente). */
    private static javax.swing.Icon colorSwatchIcon(String hex, int size) {
        Color c = null;
        try {
            if (hex != null && hex.trim().startsWith("#")) c = Color.decode(hex.trim());
        } catch (NumberFormatException ignored) { }
        final Color fill = c;
        return new javax.swing.Icon() {
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
            @Override public void paintIcon(Component comp, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (fill != null) {
                    g2.setColor(fill);
                    g2.fillRoundRect(x, y, size, size, 4, 4);
                } else {
                    g2.setColor(UIHelper.TEXT_MUTED);
                    g2.drawRoundRect(x, y, size - 1, size - 1, 4, 4);
                }
                g2.dispose();
            }
        };
    }

    private void toggleSelectedCategory() {
        var sel = selectedCategory();
        if (sel == null) return;
        UIHelper.runWithProgress(owner, "A actualizar categoria…",
                () -> {
                    owner.productCategoryApiClient.setActive(sel.id(), !sel.active());
                    return null;
                },
                ignored -> refresh(), owner::showStockError);
    }

    private void openCategoryDialog(com.phcpro.modules.comercial.dto.ProductCategoryDTO existing) {
        boolean editing = existing != null;
        JTextField codeField = new JTextField(editing ? existing.code() : "");
        JTextField nameField = new JTextField(editing ? existing.name() : "");

        // Seletor de cor profissional: amostra + escolher (JColorChooser) + limpar. Hex guardado num holder.
        final String[] colorHolder = { editing && existing.colorHex() != null ? existing.colorHex() : "" };
        JLabel swatch = new JLabel();
        swatch.setOpaque(true);
        swatch.setPreferredSize(new Dimension(40, UIHelper.FORM_CONTROL_HEIGHT));
        swatch.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER, 1, true));
        Runnable applySwatch = () -> {
            Color c = null;
            try { if (colorHolder[0].startsWith("#")) c = Color.decode(colorHolder[0]); } catch (NumberFormatException ignored) { }
            swatch.setBackground(c != null ? c : UIHelper.BG_CARD);
            swatch.setText(c == null ? "  sem cor" : "");
            swatch.setForeground(UIHelper.TEXT_MUTED);
        };
        applySwatch.run();
        ModernButton pickBtn = UIHelper.createSecondaryButton("Escolher…");
        pickBtn.setIcon(UIHelper.icon("fas-palette", 14));
        pickBtn.addActionListener(ev -> {
            Color initial = UIHelper.ACCENT_BLUE;
            try { if (colorHolder[0].startsWith("#")) initial = Color.decode(colorHolder[0]); } catch (NumberFormatException ignored) { }
            Color chosen = JColorChooser.showDialog(owner, "Cor da categoria", initial);
            if (chosen != null) {
                colorHolder[0] = String.format("#%02X%02X%02X", chosen.getRed(), chosen.getGreen(), chosen.getBlue());
                applySwatch.run();
            }
        });
        ModernButton clearBtn = UIHelper.createSecondaryButton("Limpar");
        clearBtn.setIcon(UIHelper.icon("fas-times", 14));
        clearBtn.addActionListener(ev -> { colorHolder[0] = ""; applySwatch.run(); });
        JPanel colorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        colorPanel.setOpaque(false);
        colorPanel.add(swatch);
        colorPanel.add(pickBtn);
        colorPanel.add(clearBtn);

        JPanel form = UIHelper.createDialogForm(
                "Código:", codeField,
                "Nome:", nameField,
                "Cor (opcional):", colorPanel);

        Window parent = SwingUtilities.getWindowAncestor(owner);
        ModernFormDialog dlg = new ModernFormDialog(parent,
                editing ? "Editar Categoria" : "Nova Categoria", "fas-tags",
                "Organize os produtos em categorias da loja", form);
        dlg.setSize(480, 360);
        dlg.setOnSaveAsync(() -> {
            String code = codeField.getText().trim();
            String name = nameField.getText().trim();
            if (code.isEmpty() || name.isEmpty()) {
                throw new RuntimeException("Código e nome são obrigatórios.");
            }
            var req = new com.phcpro.modules.comercial.dto.CreateProductCategoryRequest(
                    code, name, colorHolder[0].isBlank() ? null : colorHolder[0]);
            return () -> editing ? owner.productCategoryApiClient.update(existing.id(), req)
                    : owner.productCategoryApiClient.create(req);
        });
        if (dlg.showDialog()) {
            refresh();
        }
    }


    private record CategoryData(List<ProductCategoryDTO> categories, List<ProductDTO> products) {}
}
