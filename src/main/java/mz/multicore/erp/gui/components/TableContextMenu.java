package mz.multicore.erp.gui.components;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Menu de contexto (botão direito) genérico para tabelas: <b>Ver detalhes · Copiar linha · Copiar célula ·
 * Ir para o topo · Ir para o fundo</b>. Ao abrir, selecciona a linha sob o cursor.
 *
 * <p>Genérico de propósito: não depende do domínio (as acções específicas — Imprimir, Anular —
 * ficam nos botões de cada painel). Instalado centralmente por
 * {@link UIHelper#styleScrollPane(JScrollPane)}. Ver docs/UI_TABELAS_UX_SPEC.md.</p>
 */
public final class TableContextMenu {

    private static final String INSTALLED = "tableContextMenu.installed";

    private TableContextMenu() {}

    public static void install(JScrollPane scroll) {
        if (scroll == null || scroll.getViewport() == null) return;
        if (!(scroll.getViewport().getView() instanceof JTable table)) return;
        if (Boolean.TRUE.equals(scroll.getClientProperty(INSTALLED))) return;
        scroll.putClientProperty(INSTALLED, Boolean.TRUE);

        JScrollBar sb = scroll.getVerticalScrollBar();
        table.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { maybeShow(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShow(e); }

            private void maybeShow(MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 0 && !table.isRowSelected(row)) {
                    table.setRowSelectionInterval(row, row);
                }
                buildMenu(table, sb, row, col).show(table, e.getX(), e.getY());
            }
        });
    }

    private static JPopupMenu buildMenu(JTable table, JScrollBar sb, int row, int col) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem details = item("Ver detalhes", "fas-info-circle", () -> RowDetailsInspector.open(table));
        details.setEnabled(row >= 0 && !Boolean.TRUE.equals(table.getClientProperty("noRowInspector")));
        menu.add(details);
        menu.addSeparator();
        JMenuItem copyRow = item("Copiar linha", "fas-copy",
                () -> toClipboard(rowToText(rowValues(table, row))));
        JMenuItem copyCell = item("Copiar célula", "fas-clone",
                () -> toClipboard(cellToText(cellValue(table, row, col))));
        copyRow.setEnabled(row >= 0);
        copyCell.setEnabled(row >= 0 && col >= 0);
        menu.add(copyRow);
        menu.add(copyCell);
        menu.addSeparator();
        menu.add(item("Ir para o topo", "fas-angle-double-up", () -> TableNavigator.top(sb)));
        menu.add(item("Ir para o fundo", "fas-angle-double-down", () -> TableNavigator.bottom(sb)));
        return menu;
    }

    // ─── helpers puros (testáveis) ───────────────────────────────────────────

    /** Junta os valores da linha separados por tab; nulos → vazio. */
    static String rowToText(Object[] values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append('\t');
            sb.append(cellToText(values[i]));
        }
        return sb.toString();
    }

    static String cellToText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    // ─── acesso à tabela ─────────────────────────────────────────────────────

    private static Object[] rowValues(JTable table, int row) {
        if (row < 0) return new Object[0];
        int cols = table.getColumnCount();
        Object[] out = new Object[cols];
        for (int c = 0; c < cols; c++) {
            out[c] = table.getValueAt(row, c);
        }
        return out;
    }

    private static Object cellValue(JTable table, int row, int col) {
        return (row < 0 || col < 0) ? null : table.getValueAt(row, col);
    }

    private static void toClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }

    private static JMenuItem item(String text, String iconCode, Runnable action) {
        JMenuItem mi = new JMenuItem(text, UIHelper.icon(iconCode, 13, UIHelper.TEXT_LIGHT));
        mi.addActionListener(e -> action.run());
        return mi;
    }
}
