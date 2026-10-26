package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ProductLookupDialog;
import mz.multicore.erp.gui.components.ProductSearchComboBox;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static mz.multicore.erp.gui.StockTransferEditorRowCalculator.COL_PRODUCT;
import static mz.multicore.erp.gui.StockTransferEditorRowCalculator.COL_QUANTITY;

/** Coordenador de integração com leitor óptico (código/SKU) e pesquisa no catálogo de artigos. */
final class StockTransferCatalogCoordinator {

    private StockTransferCatalogCoordinator() {}

    static void openCatalog(StockPanel owner, JComponent parent, List<ProductDTO> products,
                            boolean tableEditable, Runnable stopEditing, Consumer<List<ProductDTO>> onSelected) {
        if (!tableEditable) return;
        stopEditing.run();
        List<ProductDTO> catalog = (products != null && !products.isEmpty()) ? products : owner.catalogProducts;
        if (catalog == null || catalog.isEmpty()) {
            owner.showStockNotice(FeedbackType.WARNING, "Catálogo Vazio", "Não existem produtos registados no sistema.");
            return;
        }
        List<ProductDTO> selected = ProductLookupDialog.showMulti(parent, catalog);
        if (selected != null && !selected.isEmpty()) onSelected.accept(selected);
    }

    static void applySelectedProducts(JTable table, DefaultTableModel model, List<ProductDTO> selected,
                                      Runnable stopEditing, BiConsumer<Integer, Integer> recalculateRow,
                                      BiConsumer<ProductDTO, BigDecimal> addLineRow) {
        if (selected == null || selected.isEmpty()) return;
        stopEditing.run();
        int viewRow = table.getSelectedRow(), modelRow = viewRow >= 0 ? table.convertRowIndexToModel(viewRow) : -1;
        boolean fillCurrent = (modelRow >= 0 && modelRow < model.getRowCount() && model.getValueAt(modelRow, COL_PRODUCT) == null);
        int targetModelRow = modelRow;
        for (int i = 0; i < selected.size(); i++) {
            ProductDTO p = selected.get(i);
            if (i == 0 && fillCurrent) {
                model.setValueAt(p, modelRow, COL_PRODUCT);
                recalculateRow.accept(modelRow, COL_PRODUCT);
                targetModelRow = modelRow;
            } else {
                addLineRow.accept(p, BigDecimal.ONE);
                targetModelRow = model.getRowCount() - 1;
            }
        }
        focusTargetRow(table, targetModelRow);
    }

    static void handleBarcodeScanned(String code, StockPanel owner, JTable table, DefaultTableModel model,
                                     ProductSearchComboBox cellEditor, List<ProductDTO> products,
                                     boolean tableEditable, Runnable stopEditing,
                                     BiConsumer<Integer, Integer> recalculateRow,
                                     BiConsumer<ProductDTO, BigDecimal> addLineRow) {
        if (!tableEditable) return;
        ProductDTO product = products.stream().filter(p -> code.equalsIgnoreCase(p.barcode())
                || code.equalsIgnoreCase(p.sku()) || (p.reference() != null && code.equalsIgnoreCase(p.reference()))
        ).findFirst().orElse(null);

        if (product == null) {
            Toolkit.getDefaultToolkit().beep();
            owner.showStockNotice(FeedbackType.WARNING, "Artigo não encontrado", "Nenhum produto cadastrado com o código ou SKU: " + code);
            return;
        }
        stopEditing.run();
        int existingIndex = -1;
        for (int i = 0; i < model.getRowCount(); i++) {
            ProductDTO p = cellEditor.resolve(model.getValueAt(i, COL_PRODUCT));
            if (p != null && product.id().equals(p.id())) { existingIndex = i; break; }
        }
        if (existingIndex >= 0) {
            BigDecimal currentQty = StockTransferEditorRowCalculator.decimal(model.getValueAt(existingIndex, COL_QUANTITY));
            model.setValueAt(currentQty.add(BigDecimal.ONE), existingIndex, COL_QUANTITY);
            recalculateRow.accept(existingIndex, COL_QUANTITY);
            table.setRowSelectionInterval(existingIndex, existingIndex);
        } else {
            if (model.getRowCount() == 1) {
                Object val = model.getValueAt(0, COL_PRODUCT);
                if (val == null || (val instanceof String s && s.isBlank())) model.removeRow(0);
            }
            addLineRow.accept(product, BigDecimal.ONE);
            int newRow = model.getRowCount() - 1;
            if (newRow >= 0 && newRow < table.getRowCount()) table.setRowSelectionInterval(newRow, newRow);
        }
    }

    private static void focusTargetRow(JTable table, int targetModelRow) {
        if (targetModelRow < 0) return;
        SwingUtilities.invokeLater(() -> {
            try {
                int vr = table.convertRowIndexToView(targetModelRow);
                table.setRowSelectionInterval(vr, vr);
                table.scrollRectToVisible(table.getCellRect(vr, COL_QUANTITY, true));
                table.editCellAt(vr, COL_QUANTITY);
                Component comp = table.getEditorComponent();
                if (comp != null) comp.requestFocusInWindow();
            } catch (Exception ignored) {}
        });
    }
}
