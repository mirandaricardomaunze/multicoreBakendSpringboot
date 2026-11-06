package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.QuickPeekPanel;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.ProductCategoryDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Coordenador de apresentação, renderização e Quick Peek das categorias de produto.
 */
public final class StockCategoriesViewCoordinator {

    public record CategoryData(List<ProductCategoryDTO> categories, List<ProductDTO> products) {}

    private StockCategoriesViewCoordinator() {}

    public static void applyCategories(StockPanel owner, CategoryData data) {
        owner.categoriesList = data.categories();
        owner.catalogProducts = data.products();
        owner.categoryProductCounts.clear();
        for (ProductDTO p : data.products()) {
            if (p.categoryId() != null) {
                owner.categoryProductCounts.merge(p.categoryId(), 1, Integer::sum);
            }
        }
        filterCategories(owner, owner.categorySearchField == null ? "" : owner.categorySearchField.getText());
    }

    public static void filterCategories(StockPanel owner, String query) {
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
                    c.code(), c.name(), c.colorHex() != null && !c.colorHex().isBlank() ? c.colorHex() : "—",
                    owner.categoryProductCounts.getOrDefault(c.id(), 0), c.active() ? "Activa" : "Inactiva"});
        }
    }

    public static ProductCategoryDTO selectedCategory(StockPanel owner) {
        int row = owner.categoriesTable.getSelectedRow();
        if (row < 0 || row >= owner.categoriesFiltered.size()) {
            owner.showStockNotice(FeedbackType.WARNING, "Seleccione uma categoria", "Escolha uma categoria na tabela para continuar.");
            return null;
        }
        return owner.categoriesFiltered.get(row);
    }

    public static void toggleSelectedCategory(StockPanel owner, Runnable onDone) {
        var sel = selectedCategory(owner);
        if (sel == null) return;
        UIHelper.runWithProgress(owner, "A actualizar categoria…",
                () -> {
                    owner.productCategoryApiClient.setActive(sel.id(), !sel.active());
                    return null;
                },
                ignored -> onDone.run(), owner::showStockError);
    }

    public static void populateQuickPeek(QuickPeekPanel peek, StockPanel owner, int modelRow) {
        peek.setHeaderIcon("fas-tags", UIHelper.ACCENT_BLUE);
        peek.setTitle(String.valueOf(owner.categoriesModel.getValueAt(modelRow, 1)));
        peek.setSubtitle("Código: " + owner.categoriesModel.getValueAt(modelRow, 0));
        peek.setStatus(String.valueOf(owner.categoriesModel.getValueAt(modelRow, 4)), UIHelper.ACCENT_BLUE);
        peek.setItems(List.of(
                new QuickPeekPanel.PeekItem("Código", peek.cell(owner.categoriesModel.getValueAt(modelRow, 0), 0), false),
                new QuickPeekPanel.PeekItem("Designação", peek.cell(owner.categoriesModel.getValueAt(modelRow, 1), 1), false),
                new QuickPeekPanel.PeekItem("Cor Hex", peek.cell(owner.categoriesModel.getValueAt(modelRow, 2), 2), false),
                new QuickPeekPanel.PeekItem("Total de Produtos", peek.cell(owner.categoriesModel.getValueAt(modelRow, 3), 3), true)));
    }

    public static DefaultTableCellRenderer createColorCellRenderer() {
        return new DefaultTableCellRenderer() {
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
        };
    }

    private static Icon colorSwatchIcon(String hex, int size) {
        Color c = null;
        try {
            if (hex != null && hex.trim().startsWith("#")) c = Color.decode(hex.trim());
        } catch (NumberFormatException ignored) {}
        final Color fill = c;
        return new Icon() {
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
}
