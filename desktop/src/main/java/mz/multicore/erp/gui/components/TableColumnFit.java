package mz.multicore.erp.gui.components;

import javax.swing.JTable;
import javax.swing.table.TableColumn;
import java.awt.Component;
import java.util.regex.Pattern;

/**
 * Ajusta as colunas ao conteúdo. Valores, datas e estados nunca são cortados: um número sem os
 * últimos dígitos lê-se mal. O texto livre fica com o espaço que sobra e mostra reticências.
 */
final class TableColumnFit {

    private static final int MEASURED_ROWS = 40;
    private static final int PADDING = 14;
    private static final int MAX_TEXT_WIDTH = 320;
    private static final Pattern RIGID = Pattern.compile(
            "[-+]?[\\d.,\\s]+(MT|%|un|d)?|\\d{2}/\\d{2}/\\d{4}( \\d{2}:\\d{2}(:\\d{2})?)?");

    private static final String BUSINESS_UNIT = "Negócio";
    private static final Pattern NO_BUSINESS_UNIT = Pattern.compile("|-|—|Geral|Misto");

    private TableColumnFit() {}

    static void fit(JTable table) {
        for (int column = 0; column < table.getColumnCount(); column++) {
            TableColumn tableColumn = table.getColumnModel().getColumn(column);
            if (hideUnusedBusinessUnit(table, tableColumn, column)) continue;
            if (tableColumn.getMaxWidth() < 4) continue;
            int widest = 0;
            boolean rigid = isStatus(table.getColumnName(column));
            for (int row = 0; row < Math.min(table.getRowCount(), MEASURED_ROWS); row++) {
                Object value = table.getValueAt(row, column);
                int width = rendererWidth(table, row, column);
                widest = Math.max(widest, width);
                rigid |= value instanceof Number || (value != null && RIGID.matcher(value.toString().trim()).matches());
            }
            if (widest == 0) continue;
            int needed = widest + PADDING;
            if (rigid && tableColumn.getMinWidth() < needed) tableColumn.setMinWidth(needed);
            int preferred = Math.min(needed, Math.max(MAX_TEXT_WIDTH, rigid ? needed : 0));
            if (tableColumn.getPreferredWidth() < preferred) tableColumn.setPreferredWidth(preferred);
        }
    }

    /** Numa empresa sem unidades de negócio, a coluna só repetiria "—" em todas as linhas. */
    private static boolean hideUnusedBusinessUnit(JTable table, TableColumn tableColumn, int column) {
        if (!BUSINESS_UNIT.equals(table.getColumnName(column))) return false;
        boolean unused = table.getRowCount() > 0;
        for (int row = 0; row < table.getRowCount() && unused; row++) {
            Object value = table.getValueAt(row, column);
            unused = value == null || NO_BUSINESS_UNIT.matcher(value.toString().trim()).matches();
        }
        if (unused) {
            tableColumn.setMinWidth(0);
            tableColumn.setPreferredWidth(0);
            tableColumn.setMaxWidth(0);
        } else if (tableColumn.getMaxWidth() == 0) {
            tableColumn.setMaxWidth(Integer.MAX_VALUE);
            tableColumn.setMinWidth(75);
            tableColumn.setPreferredWidth(100);
        }
        return unused;
    }

    private static boolean isStatus(String header) {
        return header != null && header.toLowerCase(java.util.Locale.ROOT).startsWith("estado");
    }

    private static int rendererWidth(JTable table, int row, int column) {
        try {
            Component renderer = table.prepareRenderer(table.getCellRenderer(row, column), row, column);
            return renderer.getPreferredSize().width;
        } catch (RuntimeException staleRow) {
            return 0;
        }
    }
}
