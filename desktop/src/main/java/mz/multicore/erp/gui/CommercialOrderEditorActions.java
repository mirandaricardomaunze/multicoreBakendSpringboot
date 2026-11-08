package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.modules.comercial.dto.CreateInvoiceLineRequest;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Interacções do editor de encomendas, coordenadas de forma desacoplada
 * com a política de crédito, coordenador de ciclo de vida e leitor de códigos de barras.
 */
final class CommercialOrderEditorActions {

    private static boolean inStopCellEditing = false;

    private CommercialOrderEditorActions() {}

    static void openSelected(ComercialPanel owner) { CommercialOrderLifecycleCoordinator.openSelected(owner); }

    static void load(ComercialPanel owner, OrderDTO order) {
        owner.resetOrderDraft();
        owner.editingOrder = order;
        owner.orderClientCombo.setSelectedIndex(CommercialOrderCreditPolicy.clientIndex(owner, order.clientId()));
        OrderEditorExtras.loadForOrder(owner, order);
        owner.orderClientWalkInField.setText(order.walkInName() == null ? "" : order.walkInName());
        if (owner.orderNotesField != null) owner.orderNotesField.setText(order.notes() == null ? "" : order.notes());
        CommercialOrderLifecycleCoordinator.selectWarehouse(owner, owner.orderWarehouseCombo, order.warehouseId());
        CommercialOrderLifecycleCoordinator.selectWarehouse(owner, owner.orderDestinationCombo, order.destinationWarehouseId());
        owner.orderKindCombo.setSelectedItem(order.kind());
        owner.orderKindCombo.setEnabled(false);
        owner.orderGridEditable = true;
        if (owner.orderPaymentTermsCombo != null) {
            if (order.paymentTerms() != null && !order.paymentTerms().isBlank()) {
                CommercialOrderCreditPolicy.ensurePaymentTermsOption(owner.orderPaymentTermsCombo, order.paymentTerms());
                owner.orderPaymentTermsCombo.setSelectedItem(order.paymentTerms());
            } else {
                owner.orderPaymentTermsCombo.setSelectedItem("Pronto Pagamento");
            }
        }
        CommercialOrderCreditPolicy.handleClientChanged(owner);
        for (mz.multicore.erp.modules.comercial.dto.OrderLineDTO line : order.lines()) {
            owner.draftOrderLines.add(new CreateInvoiceLineRequest(line.productId(), line.quantity(), line.taxRate(),
                    line.discountPercentage(), line.batchNumber(), line.serialNumber()));
            owner.draftOrderUnitPrices.add(line.unitPrice());
        }
        refreshTable(owner);
        owner.orderEditor.setEditorTitle("Editar Encomenda " + order.orderNumber());
        owner.orderEditor.setSaveText("Guardar alterações");
        owner.orderEditor.setSaveEnabled(true);
        owner.clearOrderEditorDirty();
        owner.encomendasCards.show(owner.encomendasHost, "editor");
    }
    static void handleClientChanged(ComercialPanel owner) { CommercialOrderCreditPolicy.handleClientChanged(owner); }
    static void ensurePaymentTermsOption(JComboBox<String> combo, String option) { CommercialOrderCreditPolicy.ensurePaymentTermsOption(combo, option); }
    static void handleBarcodeScanned(ComercialPanel owner, String code) { CommercialOrderBarcodeAndLookupHandler.handleBarcodeScanned(owner, code); }
    static void applyProductsFromLookup(ComercialPanel owner, List<ProductDTO> selected) { CommercialOrderBarcodeAndLookupHandler.applyProductsFromLookup(owner, selected); }

    static void addBlankLine(ComercialPanel owner) {
        if (!owner.orderGridEditable) return;
        stopCellEditing(owner);
        owner.draftOrderLines.add(new CreateInvoiceLineRequest(0L, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, null, null));
        owner.draftOrderUnitPrices.add(BigDecimal.ZERO);
        refreshTable(owner);
        int modelRow = owner.orderLinesTableModel.getRowCount() - 1, rowCount = owner.orderLinesTable.getRowCount();
        if (modelRow >= 0 && rowCount > 0) {
            int viewRow = Math.min(Math.max(0, modelRow), rowCount - 1);
            try { if (modelRow < rowCount) viewRow = owner.orderLinesTable.convertRowIndexToView(modelRow); } catch (Exception ignored) { viewRow = rowCount - 1; }
            if (viewRow >= 0 && viewRow < rowCount) {
                final int targetRow = viewRow;
                try {
                    owner.orderLinesTable.setRowSelectionInterval(targetRow, targetRow);
                    owner.orderLinesTable.scrollRectToVisible(owner.orderLinesTable.getCellRect(targetRow, 0, true));
                } catch (Exception ignored) {}
                SwingUtilities.invokeLater(() -> {
                    try {
                        if (targetRow < owner.orderLinesTable.getRowCount()) {
                            owner.orderLinesTable.editCellAt(targetRow, 0);
                            if (owner.orderLinesTable.getEditorComponent() != null) owner.orderLinesTable.getEditorComponent().requestFocusInWindow();
                        }
                    } catch (Exception ignored) {}
                });
            }
        }
        owner.markOrderEditorDirty();
    }

    static void syncLineFromGrid(ComercialPanel owner, int row, int column) {
        if (row < 0 || row >= owner.draftOrderLines.size()) return;
        CreateInvoiceLineRequest previous = owner.draftOrderLines.get(row);
        Object cellProduct = owner.orderLinesTableModel.getValueAt(row, 0);
        ProductDTO product = null;
        if (cellProduct instanceof ProductDTO selected) product = selected;
        else if (owner.orderProductCombo != null) product = owner.orderProductCombo.resolve(cellProduct);
        if (product == null && cellProduct != null) {
            String label = String.valueOf(cellProduct).trim();
            if (!label.isBlank() && !"Pesquisar produto...".equalsIgnoreCase(label)) {
                product = owner.productsList.stream().filter(p ->
                        label.equalsIgnoreCase(p.name()) || (p.reference() != null && label.equalsIgnoreCase(p.reference()))
                        || (p.sku() != null && label.equalsIgnoreCase(p.sku())) || (p.barcode() != null && label.equalsIgnoreCase(p.barcode()))
                ).findFirst().orElse(null);
            }
        }
        if (product == null && previous.productId() != null && previous.productId() > 0) {
            final Long prevId = previous.productId();
            product = owner.productsList.stream().filter(p -> prevId.equals(p.id())).findFirst().orElse(null);
        }
        int unitsPerPackage = product == null ? 1 : Math.max(1, product.unitsPerPackage());
        BigDecimal unitsPerBox = BigDecimal.valueOf(product == null ? 1L : (long) Math.max(1, product.packagesPerBox()) * unitsPerPackage);
        BigDecimal quantity;
        if (column == 2) quantity = decimal(owner.orderLinesTableModel.getValueAt(row, 2)).multiply(BigDecimal.valueOf(unitsPerPackage));
        else if (column == 3) quantity = decimal(owner.orderLinesTableModel.getValueAt(row, 3)).multiply(unitsPerBox);
        else quantity = decimal(owner.orderLinesTableModel.getValueAt(row, 1));
        BigDecimal discount = decimal(owner.orderLinesTableModel.getValueAt(row, 9));
        String serial = String.valueOf(owner.orderLinesTableModel.getValueAt(row, 10)).trim();
        if (serial.isEmpty() || "—".equals(serial)) serial = null;
        owner.draftOrderLines.set(row, new CreateInvoiceLineRequest(
                product == null ? 0L : product.id(), quantity,
                product == null ? previous.taxRate() : product.effectiveTaxRate(), discount,
                previous.batchNumber(), serial));
        owner.draftOrderUnitPrices.set(row, product == null ? BigDecimal.ZERO : previewPrice(product, quantity));
        owner.markOrderEditorDirty();
        refreshTable(owner);
    }

    static void removeSelectedLine(ComercialPanel owner) {
        int row = TableFilter.selectedModelRow(owner.orderLinesTable);
        if (row < 0 || row >= owner.draftOrderLines.size()) {
            owner.showCommercialNotice(FeedbackType.WARNING, "Seleccione um item", "Escolha na tabela o item que pretende remover.");
            return;
        }
        owner.draftOrderLines.remove(row);
        owner.draftOrderUnitPrices.remove(row);
        refreshTable(owner);
        owner.markOrderEditorDirty();
    }

    static void refreshTable(ComercialPanel owner) {
        owner.syncingOrderGrid = true;
        try {
            owner.orderLinesTableModel.setRowCount(0);
            owner.draftOrderSubtotal = BigDecimal.ZERO;
            owner.draftOrderTax = BigDecimal.ZERO;
            owner.draftOrderTotal = BigDecimal.ZERO;
            for (int index = 0; index < owner.draftOrderLines.size(); index++) {
                CreateInvoiceLineRequest line = owner.draftOrderLines.get(index);
                ProductDTO product = owner.productsList.stream().filter(item -> item.id().equals(line.productId()))
                        .findFirst().orElse(null);
                BigDecimal unitPrice = index < owner.draftOrderUnitPrices.size()
                        ? owner.draftOrderUnitPrices.get(index)
                        : product == null ? BigDecimal.ZERO : previewPrice(product, line.quantity());
                BigDecimal discount = line.discountPercentage() == null ? BigDecimal.ZERO : line.discountPercentage();
                BigDecimal subTotal = unitPrice.multiply(line.quantity());
                subTotal = subTotal.subtract(subTotal.multiply(discount).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
                BigDecimal tax = subTotal.multiply(product == null ? line.taxRate() : product.effectiveTaxRate());
                BigDecimal total = subTotal.add(tax).setScale(2, RoundingMode.HALF_UP);
                int unitsPerPackage = product == null ? 1 : Math.max(1, product.unitsPerPackage());
                BigDecimal unitsPerBox = BigDecimal.valueOf(product == null ? 1L : (long) Math.max(1, product.packagesPerBox()) * unitsPerPackage);
                BigDecimal packages = line.quantity().divide(BigDecimal.valueOf(unitsPerPackage), 2, RoundingMode.HALF_UP);
                BigDecimal boxes = line.quantity().divide(unitsPerBox, 2, RoundingMode.HALF_UP);
                BigDecimal boxPercentage = line.quantity().multiply(BigDecimal.valueOf(100)).divide(unitsPerBox, 1, RoundingMode.HALF_UP);
                owner.orderLinesTableModel.addRow(new Object[]{
                        product, line.quantity(), packages, boxes,
                        boxPercentage.stripTrailingZeros().toPlainString() + "%",
                        BigDecimal.ZERO, "0%", "0%", unitPrice, discount,
                        line.serialNumber() == null ? "" : line.serialNumber(), total
                });
                owner.draftOrderSubtotal = owner.draftOrderSubtotal.add(subTotal);
                owner.draftOrderTax = owner.draftOrderTax.add(tax);
                owner.draftOrderTotal = owner.draftOrderTotal.add(total);
            }
            OrderPackageQuantityBinding.refreshDraftLogistics(owner);
        } finally {
            owner.syncingOrderGrid = false;
        }
        owner.orderTotalLabel.setText(String.format("Total Rascunho: %,.2f MT (incl. IVA)", owner.draftOrderTotal));
    }

    static void stopCellEditing(ComercialPanel owner) {
        if (inStopCellEditing) return;
        inStopCellEditing = true;
        try {
            if (owner.orderLinesTable != null && owner.orderLinesTable.isEditing() && owner.orderLinesTable.getCellEditor() != null) {
                if (!owner.orderLinesTable.getCellEditor().stopCellEditing()) owner.orderLinesTable.getCellEditor().cancelCellEditing();
            }
        } finally {
            inStopCellEditing = false;
        }
    }

    static BigDecimal previewPrice(ProductDTO product, BigDecimal quantity) {
        if (product.wholesalePrice() != null && product.wholesaleMinQty() != null && quantity.compareTo(product.wholesaleMinQty()) >= 0) {
            return product.wholesalePrice();
        }
        return product.unitPrice();
    }

    private static BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal number) return number;
        if (value == null || String.valueOf(value).isBlank()) return BigDecimal.ZERO;
        try { return new BigDecimal(String.valueOf(value).trim().replace(',', '.')); }
        catch (NumberFormatException ignored) { return BigDecimal.ZERO; }
    }
}
