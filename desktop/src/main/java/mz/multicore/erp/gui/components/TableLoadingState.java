package mz.multicore.erp.gui.components;

import javax.swing.JTable;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

/**
 * Tells the tables of a screen that their data is on the way (docs/ESTADO_CARREGAR_SPEC.md).
 *
 * <p>{@code UIHelper.loadAsync} calls {@link #begin} on its scope and {@link #end} when the load finishes.
 * While a table is loading and still has no rows, {@link TableEmptyState} shows "A carregar…" with a busy
 * bar instead of "Sem registos.", which would be a false statement. A table that already shows rows keeps
 * them on screen, so a refresh never flickers.
 */
public final class TableLoadingState {

    /** Client property on a JTable: number of loads in flight for it. Changes fire a property event. */
    static final String COUNT = "tableLoading.count";

    private TableLoadingState() {
    }

    /** Marks every table inside the scope as loading; returns them so {@link #end} releases the same ones. */
    public static List<JTable> begin(Component scope) {
        List<JTable> tables = tablesIn(scope);
        for (JTable table : tables) {
            table.putClientProperty(COUNT, count(table) + 1);
        }
        return tables;
    }

    public static void end(List<JTable> tables) {
        if (tables == null) return;
        for (JTable table : tables) {
            int left = Math.max(0, count(table) - 1);
            table.putClientProperty(COUNT, left == 0 ? null : left);
        }
    }

    public static boolean isLoading(JTable table) {
        return count(table) > 0;
    }

    static List<JTable> tablesIn(Component scope) {
        List<JTable> found = new ArrayList<>();
        collect(scope, found);
        return found;
    }

    private static void collect(Component c, List<JTable> found) {
        if (c instanceof JTable table) {
            found.add(table);
            return;
        }
        if (c instanceof Container container) {
            for (Component child : container.getComponents()) {
                collect(child, found);
            }
        }
    }

    private static int count(JTable table) {
        return table.getClientProperty(COUNT) instanceof Integer n ? n : 0;
    }
}
