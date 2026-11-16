package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.modules.comercial.dto.CreateInvoiceLineRequest;

import javax.swing.JComponent;
import javax.swing.JTable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Narrow public view of the invoice and order draft grids for sector add-ons (SA-C02 unit entry). It only
 * reads the selected line's product and writes the base quantity into the "Qtd" cell, so the grid's own
 * synchronisation recalculates boxes, prices and totals exactly as when the operator types the quantity.
 */
public final class CommercialDraftLineAccess {

    /** Column of the base quantity in both draft grids ({@code "Qtd"}). */
    static final int QUANTITY_COLUMN = 1;

    private CommercialDraftLineAccess() {
    }

    /** The invoice and order draft grids of a Commercial panel (empty for any other component). */
    public static List<JTable> draftTables(JComponent component) {
        List<JTable> tables = new ArrayList<>();
        if (component instanceof ComercialPanel panel) {
            if (panel.linesTable != null) tables.add(panel.linesTable);
            if (panel.orderLinesTable != null) tables.add(panel.orderLinesTable);
        }
        return tables;
    }

    /** Product of the selected draft line, or {@code null} when nothing valid is selected. */
    public static Long selectedProductId(JComponent component, JTable table) {
        List<CreateInvoiceLineRequest> lines = lines(component, table);
        int row = TableFilter.selectedModelRow(table);
        if (lines == null || row < 0 || row >= lines.size()) return null;
        Long productId = lines.get(row).productId();
        return productId == null || productId <= 0 ? null : productId;
    }

    /** Writes the base quantity into the selected line; returns {@code false} if the selection is gone. */
    public static boolean setSelectedQuantity(JComponent component, JTable table, BigDecimal baseQuantity) {
        List<CreateInvoiceLineRequest> lines = lines(component, table);
        int row = TableFilter.selectedModelRow(table);
        if (lines == null || row < 0 || row >= lines.size()) return false;
        table.getModel().setValueAt(baseQuantity, row, QUANTITY_COLUMN);
        return true;
    }

    /**
     * Adds a product line through the grid's own lookup path, so prices, VAT and totals follow the usual rules.
     * Returns {@code false} when the grid is not editable or not a draft grid.
     */
    public static boolean addProduct(JComponent component, JTable table, mz.multicore.erp.modules.comercial.dto.ProductDTO product) {
        if (!(component instanceof ComercialPanel panel) || table == null || product == null) return false;
        if (table == panel.linesTable) {
            if (!panel.invoiceGridEditable) return false;
            CommercialInvoiceDraftActions.applyInvoiceProductsFromLookup(panel, List.of(product));
            return true;
        }
        if (table == panel.orderLinesTable) {
            CommercialOrderEditorActions.applyProductsFromLookup(panel, List.of(product));
            return true;
        }
        return false;
    }

    /** Draft total label of the grid ("Total Rascunho: ..."), so add-ons can show it near the line toolbar. */
    public static javax.swing.JLabel totalLabel(JComponent component, JTable table) {
        if (!(component instanceof ComercialPanel panel) || table == null) return null;
        if (table == panel.linesTable) return panel.totalLabel;
        if (table == panel.orderLinesTable) return panel.orderTotalLabel;
        return null;
    }

    /** Lines with a product and the amounts the draft editor already computed, or {@code null} for other grids. */
    public static mz.multicore.erp.gui.sector.DraftTotalsBar.Totals totals(JComponent component, JTable table) {
        List<CreateInvoiceLineRequest> lines = lines(component, table);
        if (lines == null) return null;
        ComercialPanel panel = (ComercialPanel) component;
        int count = (int) lines.stream().filter(l -> l.productId() != null && l.productId() > 0).count();
        if (table == panel.linesTable) {
            return new mz.multicore.erp.gui.sector.DraftTotalsBar.Totals(count, panel.draftSubtotal, panel.draftTax, panel.draftTotal);
        }
        return new mz.multicore.erp.gui.sector.DraftTotalsBar.Totals(count, panel.draftOrderSubtotal, panel.draftOrderTax,
                panel.draftOrderTotal);
    }

    private static List<CreateInvoiceLineRequest> lines(JComponent component, JTable table) {
        if (!(component instanceof ComercialPanel panel) || table == null) return null;
        if (table == panel.linesTable) return panel.draftLines;
        if (table == panel.orderLinesTable) return panel.draftOrderLines;
        return null;
    }
}
