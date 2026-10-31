package mz.multicore.erp.gui.components;

import javax.swing.*;
import java.awt.event.*;
import java.util.function.Function;
import java.util.function.IntConsumer;

/** Interacção por rato e teclado em grelhas: duplo clique, menu de contexto e navegação estilo folha de cálculo. */
final class UiTableInteractionSupport {

    private UiTableInteractionSupport() {}

    static void installDoubleClick(JTable table, Runnable onDoubleClick) {
        if (table == null || onDoubleClick == null) return;
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && table.getSelectedRow() >= 0) {
                    onDoubleClick.run();
                }
            }
        });
    }

    static void installRowDoubleClickHandler(JTable table, IntConsumer onRowDoubleClicked) {
        if (table == null || onRowDoubleClicked == null) return;
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int viewRow = table.rowAtPoint(e.getPoint());
                    if (viewRow >= 0 && viewRow < table.getRowCount()) {
                        onRowDoubleClicked.accept(table.convertRowIndexToModel(viewRow));
                    }
                }
            }
        });
    }

    static void installRowContextMenu(JTable table, Function<Integer, JPopupMenu> popupProvider) {
        if (table == null || popupProvider == null) return;
        table.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { maybeShowPopup(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShowPopup(e); }
            private void maybeShowPopup(MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                int viewRow = table.rowAtPoint(e.getPoint());
                if (viewRow >= 0 && viewRow < table.getRowCount()) {
                    table.setRowSelectionInterval(viewRow, viewRow);
                    JPopupMenu menu = popupProvider.apply(table.convertRowIndexToModel(viewRow));
                    if (menu != null && menu.getComponentCount() > 0 && table.isShowing()) {
                        menu.show(table, e.getX(), e.getY());
                    }
                }
            }
        });
    }

    static void installDocumentGridShortcuts(JTable table, Runnable onAdd, Runnable onRemove, Runnable onSave, Runnable onLookup) {
        if (table == null) return;
        InputMap im = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = table.getActionMap();
        if (onAdd != null) {
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), "gridAddLine");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "gridAddLine");
            am.put("gridAddLine", action(onAdd));
        }
        if (onRemove != null) {
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "gridRemoveLine");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.CTRL_DOWN_MASK), "gridRemoveLine");
            am.put("gridRemoveLine", action(() -> {
                if (!table.isEditing() && table.getSelectedRow() >= 0) onRemove.run();
            }));
        }
        if (onSave != null) {
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "gridSaveDoc");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0), "gridSaveDoc");
            am.put("gridSaveDoc", action(onSave));
        }
        if (onLookup != null) {
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "gridLookupProduct");
            am.put("gridLookupProduct", action(onLookup));
        }
        installCellNavigationKeys(table, onAdd);
    }

    static void installCellNavigationKeys(JTable table, Runnable onAddLine) {
        if (table == null) return;
        InputMap im = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = table.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "gridNavNext");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "gridNavNext");
        am.put("gridNavNext", action(() -> navigateNext(table, onAddLine)));
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK), "gridNavPrev");
        am.put("gridNavPrev", action(() -> navigatePrevious(table)));
    }

    private static void navigateNext(JTable table, Runnable onAddLine) {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        int row = table.getSelectedRow();
        int col = table.getSelectedColumn();
        if (row < 0) {
            if (table.getRowCount() > 0) {
                row = 0;
                col = findNextEditableColumn(table, row, -1);
            } else {
                if (onAddLine != null) onAddLine.run();
                return;
            }
        } else {
            int nextCol = findNextEditableColumn(table, row, col);
            if (nextCol != -1) {
                col = nextCol;
            } else if (row + 1 < table.getRowCount()) {
                row++;
                col = findNextEditableColumn(table, row, -1);
            } else if (onAddLine != null) {
                onAddLine.run();
                SwingUtilities.invokeLater(() -> {
                    int newRow = table.getRowCount() - 1;
                    if (newRow >= 0) {
                        int firstCol = findNextEditableColumn(table, newRow, -1);
                        if (firstCol >= 0) {
                            table.changeSelection(newRow, firstCol, false, false);
                            table.editCellAt(newRow, firstCol);
                        }
                    }
                });
                return;
            }
        }
        if (row >= 0 && row < table.getRowCount() && col >= 0 && col < table.getColumnCount()) {
            table.changeSelection(row, col, false, false);
            table.editCellAt(row, col);
        }
    }

    private static void navigatePrevious(JTable table) {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        int row = table.getSelectedRow();
        int col = table.getSelectedColumn();
        if (row < 0) return;
        int prevCol = findPrevEditableColumn(table, row, col);
        if (prevCol != -1) {
            col = prevCol;
        } else if (row > 0) {
            row--;
            col = findPrevEditableColumn(table, row, table.getColumnCount());
        }
        if (col >= 0 && row >= 0) {
            table.changeSelection(row, col, false, false);
            table.editCellAt(row, col);
        }
    }

    static int findNextEditableColumn(JTable table, int row, int startCol) {
        if (row < 0 || row >= table.getRowCount()) return -1;
        for (int c = startCol + 1; c < table.getColumnCount(); c++) {
            if (table.isCellEditable(row, c)) return c;
        }
        return -1;
    }

    static int findPrevEditableColumn(JTable table, int row, int startCol) {
        if (row < 0 || row >= table.getRowCount()) return -1;
        for (int c = startCol - 1; c >= 0; c--) {
            if (table.isCellEditable(row, c)) return c;
        }
        return -1;
    }

    private static AbstractAction action(Runnable r) {
        return new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { r.run(); }
        };
    }
}
