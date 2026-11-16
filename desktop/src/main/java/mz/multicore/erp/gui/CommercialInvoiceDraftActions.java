package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.CustomerCreditValidator;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ProductLookupDialog;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.comercial.dto.CreateInvoiceLineRequest;
import mz.multicore.erp.modules.comercial.dto.CreateInvoiceRequest;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Toolkit;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Operações do rascunho de faturação (linhas, grelha, códigos de barra, totais e emissão)
 * no {@link ComercialPanel}.
 */
final class CommercialInvoiceDraftActions {

    private CommercialInvoiceDraftActions() {
    }

    static void addDraftLine(ComercialPanel panel) {
        if (!panel.invoiceGridEditable) return;
        stopInvoiceCellEditing(panel);
        panel.draftLines.add(new CreateInvoiceLineRequest(
                0L, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, null, null));
        refreshInvoiceGrid(panel);
        int modelRow = panel.linesTableModel.getRowCount() - 1;
        int rowCount = panel.linesTable.getRowCount();
        if (modelRow >= 0 && rowCount > 0) {
            int viewRow = Math.min(Math.max(0, modelRow), rowCount - 1);
            try {
                if (modelRow < rowCount) {
                    viewRow = panel.linesTable.convertRowIndexToView(modelRow);
                }
            } catch (Exception ignored) {
                viewRow = rowCount - 1;
            }
            if (viewRow >= 0 && viewRow < rowCount) {
                final int targetRow = viewRow;
                try {
                    panel.linesTable.setRowSelectionInterval(targetRow, targetRow);
                    panel.linesTable.scrollRectToVisible(panel.linesTable.getCellRect(targetRow, 0, true));
                } catch (Exception ignored) {
                }
                SwingUtilities.invokeLater(() -> {
                    try {
                        if (targetRow < panel.linesTable.getRowCount()) {
                            panel.linesTable.editCellAt(targetRow, 0);
                            if (panel.linesTable.getEditorComponent() != null) {
                                panel.linesTable.getEditorComponent().requestFocusInWindow();
                            }
                        }
                    } catch (Exception ignored) {
                    }
                });
            }
        }
    }

    static void handleInvoiceBarcodeScanned(ComercialPanel panel, String code) {
        if (!panel.invoiceGridEditable) return;
        ProductDTO product = panel.productsList.stream().filter(p ->
                code.equalsIgnoreCase(p.barcode())
                        || code.equalsIgnoreCase(p.sku())
                        || (p.reference() != null && code.equalsIgnoreCase(p.reference()))
        ).findFirst().orElse(null);

        if (product == null) {
            Toolkit.getDefaultToolkit().beep();
            panel.showCommercialNotice(FeedbackType.WARNING, "Artigo não encontrado",
                    "Nenhum produto cadastrado com o código ou SKU: " + code);
            return;
        }

        int existingIndex = -1;
        for (int i = 0; i < panel.draftLines.size(); i++) {
            if (product.id().equals(panel.draftLines.get(i).productId())) {
                existingIndex = i;
                break;
            }
        }

        if (existingIndex >= 0) {
            CreateInvoiceLineRequest current = panel.draftLines.get(existingIndex);
            BigDecimal newQty = current.quantity().add(BigDecimal.ONE);
            panel.draftLines.set(existingIndex, new CreateInvoiceLineRequest(
                    product.id(), newQty, current.taxRate(), current.discountPercentage(),
                    current.batchNumber(), current.serialNumber()));
            syncInvoiceLineFromGrid(panel, existingIndex, 1);
            panel.linesTable.setRowSelectionInterval(existingIndex, existingIndex);
            panel.linesTable.scrollRectToVisible(panel.linesTable.getCellRect(existingIndex, 0, true));
        } else {
            stopInvoiceCellEditing(panel);
            panel.draftLines.add(new CreateInvoiceLineRequest(
                    product.id(), BigDecimal.ONE, product.effectiveTaxRate(), BigDecimal.ZERO, null, null));
            refreshInvoiceGrid(panel);
            int newRow = panel.linesTableModel.getRowCount() - 1;
            if (newRow >= 0) {
                panel.linesTable.setRowSelectionInterval(newRow, newRow);
                panel.linesTable.scrollRectToVisible(panel.linesTable.getCellRect(newRow, 0, true));
            }
        }
    }

    static void syncInvoiceLineFromGrid(ComercialPanel panel, int row, int column) {
        if (panel.syncingInvoiceGrid || row < 0 || row >= panel.draftLines.size()) return;
        CreateInvoiceLineRequest previous = panel.draftLines.get(row);
        Object cellProduct = panel.linesTableModel.getValueAt(row, 0);
        ProductDTO product = null;
        if (cellProduct instanceof ProductDTO selected) {
            product = selected;
        } else if (panel.productCombo != null) {
            product = panel.productCombo.resolve(cellProduct);
        }
        if (product == null && cellProduct != null) {
            String label = String.valueOf(cellProduct).trim();
            if (!label.isBlank() && !"Pesquisar produto...".equalsIgnoreCase(label)) {
                product = panel.productsList.stream().filter(p ->
                        label.equalsIgnoreCase(p.name())
                                || (p.reference() != null && label.equalsIgnoreCase(p.reference()))
                                || (p.sku() != null && label.equalsIgnoreCase(p.sku()))
                                || (p.barcode() != null && label.equalsIgnoreCase(p.barcode()))
                ).findFirst().orElse(null);
            }
        }
        if (product == null && previous.productId() != null && previous.productId() > 0) {
            final Long prevId = previous.productId();
            product = panel.productsList.stream().filter(p -> prevId.equals(p.id())).findFirst().orElse(null);
        }

        int unitsPerPackage = product == null ? 1 : Math.max(1, product.unitsPerPackage());
        BigDecimal unitsPerBox = BigDecimal.valueOf(product == null ? 1L
                : (long) Math.max(1, product.packagesPerBox()) * unitsPerPackage);
        BigDecimal quantity;
        if (column == 2) quantity = decimalGrid(panel.linesTableModel.getValueAt(row, 2))
                .multiply(BigDecimal.valueOf(unitsPerPackage));
        else if (column == 3) quantity = decimalGrid(panel.linesTableModel.getValueAt(row, 3)).multiply(unitsPerBox);
        else quantity = decimalGrid(panel.linesTableModel.getValueAt(row, 1));
        if (quantity.signum() <= 0) quantity = BigDecimal.ONE;

        BigDecimal discount = decimalGrid(panel.linesTableModel.getValueAt(row, 7));
        String serial = String.valueOf(panel.linesTableModel.getValueAt(row, 8)).trim();
        if (serial.isEmpty() || "—".equals(serial)) serial = null;
        panel.draftLines.set(row, new CreateInvoiceLineRequest(product == null ? 0L : product.id(), quantity,
                product == null ? BigDecimal.ZERO : product.effectiveTaxRate(), discount, null, serial));

        panel.syncingInvoiceGrid = true;
        try {
            BigDecimal price = product == null || product.unitPrice() == null ? BigDecimal.ZERO : product.unitPrice();
            BigDecimal subtotal = price.multiply(quantity)
                    .multiply(BigDecimal.ONE.subtract(discount.movePointLeft(2)));
            BigDecimal tax = subtotal.multiply(product == null ? BigDecimal.ZERO : product.effectiveTaxRate());
            BigDecimal total = subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);
            BigDecimal packages = quantity.divide(BigDecimal.valueOf(unitsPerPackage), 2, RoundingMode.HALF_UP);
            BigDecimal boxes = quantity.divide(unitsPerBox, 2, RoundingMode.HALF_UP);
            BigDecimal percentage = quantity.multiply(BigDecimal.valueOf(100))
                    .divide(unitsPerBox, 1, RoundingMode.HALF_UP);

            panel.linesTableModel.setValueAt(product, row, 0);
            panel.linesTableModel.setValueAt(quantity, row, 1);
            panel.linesTableModel.setValueAt(packages, row, 2);
            panel.linesTableModel.setValueAt(boxes, row, 3);
            panel.linesTableModel.setValueAt(percentage.stripTrailingZeros().toPlainString() + "%", row, 4);
            panel.linesTableModel.setValueAt(price, row, 5);
            panel.linesTableModel.setValueAt(product == null ? "0%" : product.effectiveTaxRate().movePointRight(2) + "%", row, 6);
            panel.linesTableModel.setValueAt(discount, row, 7);
            panel.linesTableModel.setValueAt(serial == null ? "" : serial, row, 8);
            panel.linesTableModel.setValueAt(total, row, 9);
        } finally {
            panel.syncingInvoiceGrid = false;
        }
        recalculateInvoiceTotals(panel);
    }

    static void removeSelectedInvoiceDraftLine(ComercialPanel panel) {
        int row = panel.linesTable.getSelectedRow();
        if (row < 0) {
            panel.showCommercialNotice(FeedbackType.WARNING, "Seleccione uma linha", "Escolha um item para remover.");
            return;
        }
        panel.draftLines.remove(panel.linesTable.convertRowIndexToModel(row));
        refreshInvoiceGrid(panel);
    }

    static void recalculateInvoiceTotals(ComercialPanel panel) {
        BigDecimal draftTotal = BigDecimal.ZERO;
        for (CreateInvoiceLineRequest line : panel.draftLines) {
            ProductDTO product = panel.productsList.stream().filter(item -> item.id().equals(line.productId()))
                    .findFirst().orElse(null);
            BigDecimal price = product == null || product.unitPrice() == null ? BigDecimal.ZERO : product.unitPrice();
            BigDecimal discount = line.discountPercentage() == null ? BigDecimal.ZERO : line.discountPercentage();
            BigDecimal subtotal = price.multiply(line.quantity())
                    .multiply(BigDecimal.ONE.subtract(discount.movePointLeft(2)));
            BigDecimal tax = subtotal.multiply(product == null ? line.taxRate() : product.effectiveTaxRate());
            BigDecimal total = subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);
            draftTotal = draftTotal.add(total);
        }
        panel.setDraftTotal(draftTotal);
        if (panel.totalLabel != null) {
            panel.totalLabel.setText(String.format("Total Rascunho: %,.2f MT (incl. IVA)", draftTotal));
        }
    }

    static void refreshInvoiceGrid(ComercialPanel panel) {
        panel.syncingInvoiceGrid = true;
        try {
            panel.linesTableModel.setRowCount(0);
            for (CreateInvoiceLineRequest line : panel.draftLines) {
                ProductDTO product = panel.productsList.stream().filter(item -> item.id().equals(line.productId()))
                        .findFirst().orElse(null);
                BigDecimal price = product == null || product.unitPrice() == null ? BigDecimal.ZERO : product.unitPrice();
                BigDecimal discount = line.discountPercentage() == null ? BigDecimal.ZERO : line.discountPercentage();
                BigDecimal subtotal = price.multiply(line.quantity())
                        .multiply(BigDecimal.ONE.subtract(discount.movePointLeft(2)));
                BigDecimal tax = subtotal.multiply(product == null ? line.taxRate() : product.effectiveTaxRate());
                BigDecimal total = subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);
                int unitsPerPackage = product == null ? 1 : Math.max(1, product.unitsPerPackage());
                BigDecimal unitsPerBox = BigDecimal.valueOf(product == null ? 1L
                        : (long) Math.max(1, product.packagesPerBox()) * unitsPerPackage);
                BigDecimal packages = line.quantity().divide(BigDecimal.valueOf(unitsPerPackage), 2, RoundingMode.HALF_UP);
                BigDecimal boxes = line.quantity().divide(unitsPerBox, 2, RoundingMode.HALF_UP);
                BigDecimal percentage = line.quantity().multiply(BigDecimal.valueOf(100))
                        .divide(unitsPerBox, 1, RoundingMode.HALF_UP);
                panel.linesTableModel.addRow(new Object[]{product, line.quantity(), packages, boxes,
                        percentage.stripTrailingZeros().toPlainString() + "%", price,
                        product == null ? "0%" : product.effectiveTaxRate().movePointRight(2) + "%",
                        discount, line.serialNumber() == null ? "" : line.serialNumber(), total});
            }
        } finally {
            panel.syncingInvoiceGrid = false;
        }
        recalculateInvoiceTotals(panel);
    }

    static CreateInvoiceRequest buildInvoiceRequest(ComercialPanel panel) {
        stopInvoiceCellEditing(panel);
        if (panel.draftLines.isEmpty()) {
            throw new RuntimeException("Adicione pelo menos um item à fatura.");
        }
        for (int row = 0; row < panel.draftLines.size(); row++) {
            CreateInvoiceLineRequest line = panel.draftLines.get(row);
            boolean productExists = panel.productsList.stream().anyMatch(product -> product.id().equals(line.productId()));
            if (!productExists) throw new RuntimeException("Seleccione o produto da linha " + (row + 1) + ".");
            if (line.quantity().signum() <= 0) throw new RuntimeException("A quantidade da linha " + (row + 1) + " deve ser positiva.");
            BigDecimal discount = line.discountPercentage() == null ? BigDecimal.ZERO : line.discountPercentage();
            if (discount.signum() < 0 || discount.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new RuntimeException("O desconto da linha " + (row + 1) + " deve ficar entre 0 e 100.");
            }
        }
        int clientIdx = panel.clientCombo.getSelectedIndex();
        int whIdx = panel.warehouseCombo.getSelectedIndex();
        if (whIdx < 0) {
            throw new RuntimeException("Selecione o armazém.");
        }
        Long clientId = null;
        String customerName = null;
        if (panel.invoiceCustomerNameField != null) {
            String typed = panel.invoiceCustomerNameField.getText().trim();
            if (!typed.isEmpty()) customerName = typed;
        }
        if (clientIdx > 0 && (clientIdx - 1) < panel.clientsList.size()) {
            ClientDTO selected = panel.clientsList.get(clientIdx - 1);
            if ("000000000".equals(selected.taxId()) || "Consumidor Final".equalsIgnoreCase(selected.name())) {
                clientId = null;
            } else {
                clientId = selected.id();
            }
        }
        WarehouseDTO warehouse = panel.warehousesList.get(whIdx);
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        Long businessUnitId = null;
        if (panel.invoiceBusinessUnitCombo != null && panel.invoiceBusinessUnitCombo.getSelectedItem() instanceof ComercialPanel.BusinessUnitOption opt) {
            businessUnitId = opt.id();
        }
        return new CreateInvoiceRequest(clientId, companyId, warehouse.id(), new ArrayList<>(panel.draftLines), null, businessUnitId, customerName);
    }

    static void resetInvoiceDraft(ComercialPanel panel) {
        panel.draftLines.clear();
        panel.setDraftTotal(BigDecimal.ZERO);
        if (panel.invoiceCustomerNameField != null) panel.invoiceCustomerNameField.setText("");
        if (panel.linesTableModel != null) panel.linesTableModel.setRowCount(0);
        if (panel.totalLabel != null) panel.totalLabel.setText("Total Rascunho: 0,00 MT (incl. IVA)");
    }

    static void openInvoiceProductLookup(ComercialPanel panel) {
        if (!panel.invoiceGridEditable) return;
        stopInvoiceCellEditing(panel);
        List<ProductDTO> selected;
        if (panel.comercialApiClient != null) {
            selected = ProductLookupDialog.showMultiRemote(
                    panel.linesTable,
                    (query, categoryId, page, size) -> panel.comercialApiClient.getProductsPage(query, categoryId, page, size)
            );
        } else {
            List<ProductDTO> catalog = panel.productsList;
            if (catalog == null || catalog.isEmpty()) {
                panel.showCommercialNotice(FeedbackType.WARNING, "Catálogo Vazio", "Não existem produtos registados no sistema.");
                return;
            }
            selected = ProductLookupDialog.showMulti(panel.linesTable, catalog);
        }
        if (selected != null && !selected.isEmpty()) {
            applyInvoiceProductsFromLookup(panel, selected);
        }
    }

    static void applyInvoiceProductsFromLookup(ComercialPanel panel, List<ProductDTO> selected) {
        if (selected == null || selected.isEmpty() || !panel.invoiceGridEditable) return;
        stopInvoiceCellEditing(panel);
        int viewRow = panel.linesTable.getSelectedRow();
        int modelRow = viewRow >= 0 ? panel.linesTable.convertRowIndexToModel(viewRow) : -1;
        boolean fillCurrent = (modelRow >= 0 && modelRow < panel.draftLines.size()
                && (panel.draftLines.get(modelRow).productId() == null || panel.draftLines.get(modelRow).productId() <= 0));

        int targetModelRow = modelRow;
        for (int i = 0; i < selected.size(); i++) {
            ProductDTO p = selected.get(i);
            if (i == 0 && fillCurrent) {
                panel.draftLines.set(modelRow, new CreateInvoiceLineRequest(
                        p.id(), BigDecimal.ONE, p.effectiveTaxRate(), BigDecimal.ZERO, null, null));
                targetModelRow = modelRow;
            } else {
                panel.draftLines.add(new CreateInvoiceLineRequest(
                        p.id(), BigDecimal.ONE, p.effectiveTaxRate(), BigDecimal.ZERO, null, null));
                targetModelRow = panel.draftLines.size() - 1;
            }
        }
        refreshInvoiceGrid(panel);
        if (targetModelRow >= 0 && targetModelRow < panel.linesTable.getRowCount()) {
            final int finalRow = targetModelRow;
            SwingUtilities.invokeLater(() -> {
                try {
                    int vr = panel.linesTable.convertRowIndexToView(finalRow);
                    panel.linesTable.setRowSelectionInterval(vr, vr);
                    panel.linesTable.scrollRectToVisible(panel.linesTable.getCellRect(vr, 1, true));
                    panel.linesTable.editCellAt(vr, 1);
                    Component comp = panel.linesTable.getEditorComponent();
                    if (comp != null) comp.requestFocusInWindow();
                } catch (Exception ignored) {
                }
            });
        }
    }

    static void stopInvoiceCellEditing(ComercialPanel panel) {
        if (panel.inStopInvoiceCellEditing) return;
        panel.inStopInvoiceCellEditing = true;
        try {
            if (panel.linesTable != null && panel.linesTable.isEditing() && panel.linesTable.getCellEditor() != null) {
                if (!panel.linesTable.getCellEditor().stopCellEditing()) {
                    panel.linesTable.getCellEditor().cancelCellEditing();
                }
            }
        } finally {
            panel.inStopInvoiceCellEditing = false;
        }
    }

    static void saveInvoiceFromEditor(ComercialPanel panel) {
        stopInvoiceCellEditing(panel);
        try {
            int clientIdx = panel.clientCombo.getSelectedIndex();
            if (clientIdx > 0 && (clientIdx - 1) < panel.clientsList.size()) {
                ClientDTO client = panel.clientsList.get(clientIdx - 1);
                CustomerCreditValidator.CreditAssessment assessment =
                        CustomerCreditValidator.validateCreditPreFlight(client, panel.getDraftTotal());
                if (!assessment.isApproved()) {
                    panel.showCommercialNotice(FeedbackType.WARNING, "Crédito não autorizado", assessment.details());
                    return;
                }
            }
            CreateInvoiceRequest request = buildInvoiceRequest(panel);
            UIHelper.runWithProgress(panel, "A emitir fatura…", () -> panel.comercialApiClient.createInvoice(request), created -> {
                panel.setLastCreatedInvoice(created);
                mz.multicore.erp.gui.sector.AdvanceReminder.afterInvoice(panel, created);
                resetInvoiceDraft(panel);
                if (created.status() == InvoiceStatus.PENDING_DISCOUNT_APPROVAL) {
                    panel.showCommercialNotice(FeedbackType.WARNING, "Fatura aguarda aprovação",
                            "Fatura " + created.invoiceNumber() + " emitida com desconto superior a 10%. Valor: "
                                    + created.totalAmount() + " MT.");
                } else {
                    ToastManager.success(panel, "Fatura " + created.invoiceNumber() + " emitida · " + created.totalAmount() + " MT.");
                }
                panel.loadInvoicesTable();
                panel.backToInvoicesList();
            }, error -> panel.showCommercialError("emitir fatura", error));
        } catch (Exception ex) {
            panel.showCommercialNotice(FeedbackType.ERROR, "Não foi possível emitir a fatura",
                    ex.getMessage() == null ? "Tente novamente." : ex.getMessage());
        }
    }

    private static BigDecimal decimalGrid(Object value) {
        if (value instanceof BigDecimal number) return number;
        if (value == null || String.valueOf(value).isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(String.valueOf(value).trim().replace(',', '.'));
        } catch (NumberFormatException ignored) {
            return BigDecimal.ZERO;
        }
    }
}
