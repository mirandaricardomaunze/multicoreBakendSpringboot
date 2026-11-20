package mz.multicore.erp.gui.components;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.beans.PropertyChangeListener;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Keeps the page and the selected row of a table across an automatic reload, matching the row by
 * its document key instead of its position (docs/ACTUALIZACAO_AUTOMATICA_SPEC.md §4.4).
 */
public final class TableStateKeeper {

    static final long RESTORE_TIMEOUT_MS = 15_000;
    private static final List<String> KEY_COLUMNS = List.of("no", "n", "numero", "documento", "codigo", "sku", "id");

    private TableStateKeeper() {
    }

    /** Page and selected-row key at capture time. */
    public record Snapshot(int page, int keyColumn, Object selectedKey) {
    }

    public static Snapshot capture(JTable table) {
        int keyColumn = keyColumn(table.getModel());
        Object key = null;
        int viewRow = table.getSelectedRow();
        if (viewRow >= 0 && keyColumn >= 0) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            key = table.getModel().getValueAt(modelRow, keyColumn);
        }
        return new Snapshot(ClientTablePagination.currentPage(table), keyColumn, key);
    }

    /** Puts back the page and reselects the row with the same key; no key match clears the selection. */
    public static void restore(JTable table, Snapshot snapshot) {
        if (snapshot.page() >= 0) ClientTablePagination.showPage(table, snapshot.page());
        if (snapshot.selectedKey() == null) return;
        TableModel model = table.getModel();
        int keyColumn = keyColumn(model);
        if (keyColumn < 0) return;
        for (int modelRow = 0; modelRow < model.getRowCount(); modelRow++) {
            if (Objects.equals(snapshot.selectedKey(), model.getValueAt(modelRow, keyColumn))) {
                int viewRow = table.convertRowIndexToView(modelRow);
                if (viewRow >= 0) {
                    table.getSelectionModel().setSelectionInterval(viewRow, viewRow);
                    table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
                }
                return;
            }
        }
        table.clearSelection();
    }

    /**
     * Restores once, after the reload's new data reaches the table (data change or a new model),
     * or gives up after {@link #RESTORE_TIMEOUT_MS}. Two invokeLater hops let the pagination apply
     * its own filter first, which would otherwise clear the selection we put back.
     */
    public static void restoreAfterReload(JTable table, Snapshot snapshot) {
        long deadline = System.currentTimeMillis() + RESTORE_TIMEOUT_MS;
        Runnable[] cleanup = new Runnable[1];
        Runnable apply = () -> {
            cleanup[0].run();
            if (System.currentTimeMillis() <= deadline) {
                SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(() -> restore(table, snapshot)));
            }
        };
        TableModel original = table.getModel();
        TableModelListener dataListener = event -> apply.run();
        PropertyChangeListener modelListener = event -> apply.run();
        cleanup[0] = () -> {
            original.removeTableModelListener(dataListener);
            table.removePropertyChangeListener("model", modelListener);
        };
        original.addTableModelListener(dataListener);
        table.addPropertyChangeListener("model", modelListener);
        javax.swing.Timer expiry = new javax.swing.Timer((int) RESTORE_TIMEOUT_MS, event -> cleanup[0].run());
        expiry.setRepeats(false);
        expiry.start();
    }

    static int keyColumn(TableModel model) {
        if (model.getColumnCount() == 0) return -1;
        for (String wanted : KEY_COLUMNS) {
            for (int column = 0; column < model.getColumnCount(); column++) {
                if (wanted.equals(normalize(model.getColumnName(column)))) return column;
            }
        }
        return 0;
    }

    private static String normalize(String name) {
        if (name == null) return "";
        String plain = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return plain.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
    }
}
