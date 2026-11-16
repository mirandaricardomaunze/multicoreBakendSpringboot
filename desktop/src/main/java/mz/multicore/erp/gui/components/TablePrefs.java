package mz.multicore.erp.gui.components;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.event.RowSorterEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Remembers, per table, the column widths, the column order and the sort the operator chose
 * (docs/MEMORIA_ECRA_SPEC.md).
 *
 * <p>Installed by {@code UIHelper.styleTable}, so no screen needs code of its own. A table is identified by
 * its column names in model order. Only what the operator does is saved (releasing the mouse on the header
 * after a resize or move, or changing the sort), never the automatic resizing that follows the window.
 * The saved layout is restored the first time the table is shown, after the screen set its defaults.
 * "Repor colunas" in the header menu forgets it.
 */
public final class TablePrefs {

    static final String RESET_LABEL = "Repor colunas";
    private static final String INSTALLED = "tablePrefs.installed";
    private static final String RESTORED = "tablePrefs.restoredFor";
    private static final String DEFAULTS = "tablePrefs.defaults";
    private static final String DEFAULT_SORT = "tablePrefs.defaultSort";
    private static final String NODE = "mz/multicore/erp/ui/tables";

    private static Preferences store = Preferences.userRoot().node(NODE);

    private TablePrefs() {
    }

    /** For tests: keep preferences in a throw-away node. */
    static void useStore(Preferences preferences) {
        store = preferences;
    }

    public static void install(JTable table) {
        if (table == null || Boolean.TRUE.equals(table.getClientProperty(INSTALLED))) return;
        table.putClientProperty(INSTALLED, Boolean.TRUE);

        table.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && table.isShowing()) restoreOnce(table);
        });
        table.addPropertyChangeListener("model", e -> table.putClientProperty(RESTORED, null));
        table.addPropertyChangeListener("rowSorter", e -> listenToSorter(table));
        listenToSorter(table);

        JTableHeader header = table.getTableHeader();
        if (header != null) {
            header.addMouseListener(new MouseAdapter() {
                @Override public void mouseReleased(MouseEvent e) {
                    if (header.getResizingColumn() != null || header.getDraggedColumn() != null) save(table);
                }
            });
            addResetItem(table, header);
        }
    }

    /** The identity of a table: its column names in model order. Null while it has no columns. */
    static String key(JTable table) {
        TableModel model = table.getModel();
        if (model == null || model.getColumnCount() == 0) return null;
        StringBuilder names = new StringBuilder();
        for (int c = 0; c < model.getColumnCount(); c++) names.append(model.getColumnName(c)).append('|');
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(names.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 12);
        } catch (NoSuchAlgorithmException impossible) {
            return Integer.toHexString(names.toString().hashCode());
        }
    }

    static void save(JTable table) {
        String key = key(table);
        if (key == null) return;
        store.put(key + ".cols", layoutOf(table));
        RowSorter<?> sorter = table.getRowSorter();
        if (sorter != null) {
            StringBuilder sort = new StringBuilder();
            for (RowSorter.SortKey sortKey : sorter.getSortKeys()) {
                if (sortKey.getSortOrder() == SortOrder.UNSORTED) continue;
                if (sort.length() > 0) sort.append(',');
                sort.append(sortKey.getColumn()).append(':').append(sortKey.getSortOrder().name());
            }
            store.put(key + ".sort", sort.toString());
        }
    }

    private static String layoutOf(JTable table) {
        TableColumnModel columns = table.getColumnModel();
        StringBuilder layout = new StringBuilder();
        for (int v = 0; v < columns.getColumnCount(); v++) {
            TableColumn column = columns.getColumn(v);
            if (layout.length() > 0) layout.append(',');
            layout.append(column.getModelIndex()).append(':').append(column.getWidth());
        }
        return layout.toString();
    }

    static void restore(JTable table) {
        String key = key(table);
        if (key == null) return;
        restoreColumns(table, store.get(key + ".cols", null));
        restoreSort(table, store.get(key + ".sort", null));
    }

    static void reset(JTable table) {
        String key = key(table);
        if (key == null) return;
        store.remove(key + ".cols");
        store.remove(key + ".sort");
        // Back to the screen's own layout, captured before the first restore (renderers stay on the columns).
        if (table.getClientProperty(DEFAULTS) instanceof String defaults) restoreColumns(table, defaults);
        if (table.getRowSorter() != null) {
            table.getRowSorter().setSortKeys(table.getClientProperty(DEFAULT_SORT) instanceof List<?> keys
                    ? castKeys(keys) : null);
        }
    }

    private static void restoreOnce(JTable table) {
        String key = key(table);
        if (key == null || key.equals(table.getClientProperty(RESTORED))) return;
        table.putClientProperty(DEFAULTS, layoutOf(table));
        table.putClientProperty(DEFAULT_SORT, table.getRowSorter() == null ? null
                : new ArrayList<>(table.getRowSorter().getSortKeys()));
        table.putClientProperty(RESTORED, key);
        restore(table);
    }

    @SuppressWarnings("unchecked")
    private static List<RowSorter.SortKey> castKeys(List<?> keys) {
        return (List<RowSorter.SortKey>) keys;
    }

    private static void restoreColumns(JTable table, String layout) {
        if (layout == null || layout.isBlank()) return;
        TableColumnModel columns = table.getColumnModel();
        int target = 0;
        for (String part : layout.split(",")) {
            String[] pair = part.split(":");
            if (pair.length != 2) return;
            int modelIndex;
            int width;
            try {
                modelIndex = Integer.parseInt(pair[0]);
                width = Integer.parseInt(pair[1]);
            } catch (NumberFormatException corrupt) {
                return;
            }
            int current = viewIndexOf(columns, modelIndex);
            if (current < 0) continue; // the screen hides this column now
            if (current != target && target < columns.getColumnCount()) columns.moveColumn(current, target);
            TableColumn column = columns.getColumn(target);
            if (width > 0) column.setPreferredWidth(Math.max(width, column.getMinWidth()));
            target++;
        }
    }

    private static void restoreSort(JTable table, String sort) {
        RowSorter<?> sorter = table.getRowSorter();
        if (sorter == null || sort == null || sort.isBlank()) return;
        List<RowSorter.SortKey> keys = new ArrayList<>();
        for (String part : sort.split(",")) {
            String[] pair = part.split(":");
            try {
                int column = Integer.parseInt(pair[0]);
                if (column >= 0 && column < table.getModel().getColumnCount()) {
                    keys.add(new RowSorter.SortKey(column, SortOrder.valueOf(pair[1])));
                }
            } catch (RuntimeException corrupt) {
                return;
            }
        }
        if (!keys.isEmpty()) sorter.setSortKeys(keys);
    }

    private static int viewIndexOf(TableColumnModel columns, int modelIndex) {
        for (int v = 0; v < columns.getColumnCount(); v++) {
            if (columns.getColumn(v).getModelIndex() == modelIndex) return v;
        }
        return -1;
    }

    private static void listenToSorter(JTable table) {
        RowSorter<?> sorter = table.getRowSorter();
        if (sorter == null || Boolean.TRUE.equals(table.getClientProperty("tablePrefs.sorter." + System.identityHashCode(sorter)))) return;
        table.putClientProperty("tablePrefs.sorter." + System.identityHashCode(sorter), Boolean.TRUE);
        sorter.addRowSorterListener(e -> {
            // Only after the first restore, so the screen's own default sort is not saved as a choice.
            if (e.getType() == RowSorterEvent.Type.SORT_ORDER_CHANGED && table.getClientProperty(RESTORED) != null) {
                save(table);
            }
        });
    }

    private static void addResetItem(JTable table, JTableHeader header) {
        JPopupMenu menu = header.getComponentPopupMenu();
        if (menu == null) {
            menu = new JPopupMenu();
            header.setComponentPopupMenu(menu);
        }
        JMenuItem reset = new JMenuItem(RESET_LABEL, UIHelper.icon("fas-undo", 12));
        reset.addActionListener(e -> reset(table));
        menu.add(reset);
    }
}
