package mz.multicore.erp.gui.components;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.function.Function;
import java.util.function.IntConsumer;

/**
 * Fachada de estilização de abas e tabelas usada por {@link UIHelper}. Delega para
 * {@link UiTabbedPaneStyler}, {@link UiTableCellStyler}, {@link UiTableInteractionSupport} e
 * {@link UiTableFooterSupport}; mantém aqui só o estilo de scroll e a calibração de colunas.
 */
final class UiTabbedAndTableStyler {

    private UiTabbedAndTableStyler() {}

    static void styleTabbedPane(JTabbedPane tabbedPane) { UiTabbedPaneStyler.styleTabbedPane(tabbedPane); }
    static void styleTabbedPaneMulticore(JTabbedPane tabbedPane) { UiTabbedPaneStyler.styleTabbedPaneMulticore(tabbedPane); }

    static void styleTable(JTable table) {
        UiTableCellStyler.applyBaseAppearance(table);
        UiTableCellStyler.installHeaderRenderer(table);
        UiTableCellStyler.installCellRenderer(table);
        RowDetailsInspector.install(table);
        UiTableFooterSupport.installRowSelector(table);
        UiTableFooterSupport.installListingFooter(table);
        TableHorizontalScrollSupport.install(table);
    }

    static void installDoubleClick(JTable table, Runnable onDoubleClick) { UiTableInteractionSupport.installDoubleClick(table, onDoubleClick); }
    static void installRowDoubleClickHandler(JTable table, IntConsumer handler) { UiTableInteractionSupport.installRowDoubleClickHandler(table, handler); }
    static void installRowContextMenu(JTable table, Function<Integer, JPopupMenu> provider) { UiTableInteractionSupport.installRowContextMenu(table, provider); }
    static void installDocumentGridShortcuts(JTable table, Runnable onAdd, Runnable onRemove, Runnable onSave, Runnable onLookup) {
        UiTableInteractionSupport.installDocumentGridShortcuts(table, onAdd, onRemove, onSave, onLookup);
    }
    static void installCellNavigationKeys(JTable table, Runnable onAddLine) { UiTableInteractionSupport.installCellNavigationKeys(table, onAddLine); }
    static int findNextEditableColumn(JTable table, int row, int startCol) { return UiTableInteractionSupport.findNextEditableColumn(table, row, startCol); }
    static int findPrevEditableColumn(JTable table, int row, int startCol) { return UiTableInteractionSupport.findPrevEditableColumn(table, row, startCol); }

    static boolean looksNumeric(String raw) { return UiTableCellStyler.looksNumeric(raw); }
    static boolean isNumericColumn(JTable table, int viewCol) { return UiTableCellStyler.isNumericColumn(table, viewCol); }

    static void styleScrollPane(JScrollPane scroll) {
        scroll.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER, 1));
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUI(new SlimScrollBarUI());
        scroll.getVerticalScrollBar().setOpaque(false);
        scroll.getHorizontalScrollBar().setUI(new SlimScrollBarUI());
        scroll.getHorizontalScrollBar().setOpaque(false);
        TableNavigator.install(scroll);
        TableEmptyState.install(scroll);
        TableContextMenu.install(scroll);
        TableHorizontalScrollSupport.install(scroll);
    }

    static void styleEmbeddedTableScrollPane(JScrollPane scroll, JTable table, int visibleRows) {
        styleScrollPane(scroll);
        table.setFillsViewportHeight(true);
        table.setPreferredScrollableViewportSize(new Dimension(420, table.getRowHeight() * visibleRows));
        int headerHeight = table.getTableHeader() != null ? table.getTableHeader().getPreferredSize().height : 0;
        int height = headerHeight + (table.getRowHeight() * visibleRows) + 26;
        scroll.setPreferredSize(new Dimension(420, height));
        scroll.setMinimumSize(new Dimension(280, height));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
    }

    static void ensureHeadersFit(JTable table) {
        FontMetrics metrics = table.getTableHeader().getFontMetrics(table.getTableHeader().getFont());
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            Object title = column.getHeaderValue();
            if (title == null) continue;
            int needed = metrics.stringWidth(title.toString()) + 24;
            if (column.getMinWidth() < needed) column.setMinWidth(needed);
            if (column.getPreferredWidth() < needed) column.setPreferredWidth(needed);
        }
        TableHorizontalScrollSupport.update(table);
    }

    static void ensureColumnFits(JTable table, int column, String widestValue) {
        if (column < 0 || column >= table.getColumnModel().getColumnCount()) return;
        int needed = table.getFontMetrics(table.getFont()).stringWidth(widestValue) + 24;
        TableColumn target = table.getColumnModel().getColumn(column);
        if (target.getMinWidth() < needed) target.setMinWidth(needed);
        if (target.getPreferredWidth() < needed) target.setPreferredWidth(needed);
    }
}
