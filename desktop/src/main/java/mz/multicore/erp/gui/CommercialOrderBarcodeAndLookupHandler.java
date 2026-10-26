package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.modules.comercial.dto.CreateInvoiceLineRequest;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import javax.swing.SwingUtilities;
import java.math.BigDecimal;
import java.util.List;

/**
 * Gestor de leitura rápida por código de barras / SKU e aplicação de artigos da pesquisa em lote.
 */
final class CommercialOrderBarcodeAndLookupHandler {

    private CommercialOrderBarcodeAndLookupHandler() {}

    static void handleBarcodeScanned(ComercialPanel owner, String code) {
        if (!owner.orderGridEditable) return;
        ProductDTO product = owner.productsList.stream().filter(p ->
                code.equalsIgnoreCase(p.barcode()) || code.equalsIgnoreCase(p.sku())
                || (p.reference() != null && code.equalsIgnoreCase(p.reference()))
        ).findFirst().orElse(null);

        if (product == null) {
            java.awt.Toolkit.getDefaultToolkit().beep();
            owner.showCommercialNotice(FeedbackType.WARNING, "Artigo não encontrado", "Nenhum produto cadastrado com o código ou SKU: " + code);
            return;
        }

        int existingIndex = -1;
        for (int i = 0; i < owner.draftOrderLines.size(); i++) {
            if (product.id().equals(owner.draftOrderLines.get(i).productId())) { existingIndex = i; break; }
        }

        if (existingIndex >= 0) {
            CreateInvoiceLineRequest current = owner.draftOrderLines.get(existingIndex);
            BigDecimal newQty = current.quantity().add(BigDecimal.ONE);
            owner.draftOrderLines.set(existingIndex, new CreateInvoiceLineRequest(
                    product.id(), newQty, current.taxRate(), current.discountPercentage(), current.batchNumber(), current.serialNumber()));
            owner.markOrderEditorDirty();
            CommercialOrderEditorActions.refreshTable(owner);
            owner.orderLinesTable.setRowSelectionInterval(existingIndex, existingIndex);
            owner.orderLinesTable.scrollRectToVisible(owner.orderLinesTable.getCellRect(existingIndex, 0, true));
        } else {
            CommercialOrderEditorActions.stopCellEditing(owner);
            owner.draftOrderLines.add(new CreateInvoiceLineRequest(product.id(), BigDecimal.ONE, product.effectiveTaxRate(), BigDecimal.ZERO, null, null));
            owner.draftOrderUnitPrices.add(CommercialOrderEditorActions.previewPrice(product, BigDecimal.ONE));
            CommercialOrderEditorActions.refreshTable(owner);
            owner.markOrderEditorDirty();
            int newRow = owner.orderLinesTableModel.getRowCount() - 1;
            if (newRow >= 0) {
                owner.orderLinesTable.setRowSelectionInterval(newRow, newRow);
                owner.orderLinesTable.scrollRectToVisible(owner.orderLinesTable.getCellRect(newRow, 0, true));
            }
        }
    }

    static void applyProductsFromLookup(ComercialPanel owner, List<ProductDTO> selected) {
        if (selected == null || selected.isEmpty() || !owner.orderGridEditable) return;
        CommercialOrderEditorActions.stopCellEditing(owner);
        int viewRow = owner.orderLinesTable.getSelectedRow();
        int modelRow = viewRow >= 0 ? owner.orderLinesTable.convertRowIndexToModel(viewRow) : -1;
        boolean fillCurrent = (modelRow >= 0 && modelRow < owner.draftOrderLines.size()
                && (owner.draftOrderLines.get(modelRow).productId() == null || owner.draftOrderLines.get(modelRow).productId() <= 0));

        int targetModelRow = modelRow;
        for (int i = 0; i < selected.size(); i++) {
            ProductDTO p = selected.get(i);
            BigDecimal price = CommercialOrderEditorActions.previewPrice(p, BigDecimal.ONE);
            if (i == 0 && fillCurrent) {
                owner.draftOrderLines.set(modelRow, new CreateInvoiceLineRequest(p.id(), BigDecimal.ONE, p.effectiveTaxRate(), BigDecimal.ZERO, null, null));
                owner.draftOrderUnitPrices.set(modelRow, price);
                targetModelRow = modelRow;
            } else {
                owner.draftOrderLines.add(new CreateInvoiceLineRequest(p.id(), BigDecimal.ONE, p.effectiveTaxRate(), BigDecimal.ZERO, null, null));
                owner.draftOrderUnitPrices.add(price);
                targetModelRow = owner.draftOrderLines.size() - 1;
            }
        }
        CommercialOrderEditorActions.refreshTable(owner);
        owner.markOrderEditorDirty();
        if (targetModelRow >= 0 && targetModelRow < owner.orderLinesTable.getRowCount()) {
            final int finalRow = targetModelRow;
            SwingUtilities.invokeLater(() -> {
                try {
                    int vr = owner.orderLinesTable.convertRowIndexToView(finalRow);
                    owner.orderLinesTable.setRowSelectionInterval(vr, vr);
                    owner.orderLinesTable.scrollRectToVisible(owner.orderLinesTable.getCellRect(vr, 1, true));
                    owner.orderLinesTable.editCellAt(vr, 1);
                    java.awt.Component comp = owner.orderLinesTable.getEditorComponent();
                    if (comp != null) comp.requestFocusInWindow();
                } catch (Exception ignored) {}
            });
        }
    }
}
