package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.purchases.dto.CreatePurchaseOrderLineRequest;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Coordenador de grelha e edição directa de linhas da encomenda. */
final class PurchaseOrderEditorCoordinator {

    private final ComprasPanel owner;
    private final PurchaseOrdersPanel panel;

    PurchaseOrderEditorCoordinator(ComprasPanel owner, PurchaseOrdersPanel panel) {
        this.owner = owner;
        this.panel = panel;
    }

    void configureProductCellEditor(JTable poLinesTable) {
        if (poLinesTable == null || poLinesTable.getColumnCount() == 0) return;
        ProductSearchComboBox poCellSearchCombo = new ProductSearchComboBox(owner.productsList);
        poCellSearchCombo.setOnLookupRequested(() -> {
            panel.stopPoCellEditing();
            openPoProductLookup(poLinesTable);
        });
        poLinesTable.getColumnModel().getColumn(0).setCellEditor(ProductSearchComboBox.createTableCellEditor(poCellSearchCombo));
    }

    void openPoProductLookup(JTable poLinesTable) {
        if (!panel.isLineGridEditable()) return;
        panel.stopPoCellEditing();
        Window win = SwingUtilities.getWindowAncestor(poLinesTable != null ? poLinesTable : owner);
        Frame frame = win instanceof Frame f ? f : null;
        ProductLookupDialog.show(frame, owner.productsList, selected -> applySelectedPoProducts(poLinesTable, selected));
    }

    private void applySelectedPoProducts(JTable poLinesTable, List<ProductDTO> selected) {
        if (selected == null || selected.isEmpty() || !panel.isLineGridEditable()) return;
        panel.stopPoCellEditing();
        int viewRow = poLinesTable != null ? poLinesTable.getSelectedRow() : -1;
        int modelRow = (viewRow >= 0 && poLinesTable != null) ? poLinesTable.convertRowIndexToModel(viewRow) : -1;
        boolean fillCurrent = (modelRow >= 0 && modelRow < owner.poDraftLines.size()
                && (owner.poDraftLines.get(modelRow).productId() == null || owner.poDraftLines.get(modelRow).productId() <= 0L));

        int targetModelRow = modelRow;
        for (int i = 0; i < selected.size(); i++) {
            ProductDTO p = selected.get(i);
            if (p == null) continue;
            BigDecimal price = p.purchasePrice() != null ? p.purchasePrice() : (p.unitPrice() != null ? p.unitPrice() : BigDecimal.ZERO);
            if (i == 0 && fillCurrent) {
                owner.poDraftLines.set(modelRow, new CreatePurchaseOrderLineRequest(
                        p.id(), BigDecimal.ONE, price, null, null, null));
                targetModelRow = modelRow;
            } else {
                owner.poDraftLines.add(new CreatePurchaseOrderLineRequest(
                        p.id(), BigDecimal.ONE, price, null, null, null));
                targetModelRow = owner.poDraftLines.size() - 1;
            }
        }
        panel.refreshPoRows();
        panel.setEditorDirty(true);
        if (targetModelRow >= 0 && poLinesTable != null && targetModelRow < poLinesTable.getRowCount()) {
            final int finalRow = targetModelRow;
            SwingUtilities.invokeLater(() -> {
                try {
                    int vr = poLinesTable.convertRowIndexToView(finalRow);
                    poLinesTable.setRowSelectionInterval(vr, vr);
                    poLinesTable.scrollRectToVisible(poLinesTable.getCellRect(vr, 1, true));
                    poLinesTable.editCellAt(vr, 1);
                    Component comp = poLinesTable.getEditorComponent();
                    if (comp != null) comp.requestFocusInWindow();
                } catch (Exception ignored) {
                }
            });
        }
    }

    void addBlankPoLine(JTable poLinesTable) {
        if (!panel.isLineGridEditable()) return;
        panel.stopPoCellEditing();
        ProductDTO product = owner.productsList.isEmpty() ? null : owner.productsList.get(0);
        owner.poDraftLines.add(new CreatePurchaseOrderLineRequest(
                product == null ? 0L : product.id(), BigDecimal.ONE, BigDecimal.ZERO,
                null, null, null));
        panel.refreshPoRows();
        panel.setEditorDirty(true);
        int row = poLinesTable.getRowCount() - 1;
        if (row >= 0) {
            poLinesTable.setRowSelectionInterval(row, row);
            poLinesTable.editCellAt(row, 0);
        }
    }

    void removeSelectedPoLine(JTable poLinesTable) {
        if (!panel.isLineGridEditable()) return;
        panel.stopPoCellEditing();
        int viewRow = poLinesTable.getSelectedRow();
        if (viewRow < 0) return;
        int row = poLinesTable.convertRowIndexToModel(viewRow);
        if (row >= 0 && row < owner.poDraftLines.size()) {
            owner.poDraftLines.remove(row);
            panel.refreshPoRows();
            panel.setEditorDirty(true);
        }
    }

    ProductDTO productById(Long id) {
        return owner.productsList.stream().filter(product -> product.id().equals(id)).findFirst().orElse(null);
    }

    BigDecimal decimalAt(int row, int column) {
        Object value = owner.poLinesModel.getValueAt(row, column);
        if (value == null || String.valueOf(value).isBlank()) return BigDecimal.ZERO;
        try { return new BigDecimal(String.valueOf(value).trim().replace(',', '.')); }
        catch (NumberFormatException ignored) { return BigDecimal.ZERO; }
    }

    String textAt(int row, int column) {
        String value = String.valueOf(owner.poLinesModel.getValueAt(row, column)).trim();
        return value.isBlank() || "null".equals(value) ? null : value;
    }

    LocalDate dateAt(int row, int column) {
        String value = textAt(row, column);
        if (value == null) return null;
        try { return LocalDate.parse(value); }
        catch (DateTimeParseException error) { throw new RuntimeException("Validade inválida na linha " + (row + 1) + ". Use aaaa-MM-dd."); }
    }

    ModernPanel buildDraftCard(JScrollPane linesScroll, JPanel poFooter,
                               Runnable addBlankAction, Runnable removeAction, Runnable lookupAction) {
        ModernPanel draftCard = new ModernPanel(16);
        draftCard.setLayout(new BorderLayout(0, 10));
        draftCard.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        ModernButton addGridLineBtn = UIHelper.createAddLineButton();
        addGridLineBtn.setText("Adicionar linha");
        addGridLineBtn.addActionListener(event -> addBlankAction.run());
        ModernButton removeGridLineBtn = UIHelper.createDangerButton("Remover item");
        removeGridLineBtn.setIcon(UIHelper.icon("fas-trash", 14));
        removeGridLineBtn.addActionListener(event -> removeAction.run());
        ModernButton lookupBtn = UIHelper.createProductLookupButton(lookupAction::run);
        JLabel gridHint = new JLabel("Edição directa na grelha: Produto, Qtd, Emb., Cx., preço, lote, validade e série.");
        gridHint.setForeground(UIHelper.TEXT_MUTED);
        draftCard.add(UIHelper.tableCardTop("Linhas da Encomenda", gridHint,
                lookupBtn, addGridLineBtn, removeGridLineBtn), BorderLayout.NORTH);
        draftCard.add(linesScroll, BorderLayout.CENTER);
        draftCard.add(poFooter, BorderLayout.SOUTH);
        return draftCard;
    }
}
