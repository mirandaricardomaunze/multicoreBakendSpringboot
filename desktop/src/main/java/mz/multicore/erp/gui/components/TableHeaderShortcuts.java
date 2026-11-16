package mz.multicore.erp.gui.components;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.function.Supplier;

/**
 * Same keys on every screen with a table header: Ctrl+N runs the main (green) action, F5 refreshes,
 * Ctrl+F jumps to the search field. Bound to the header with WHEN_IN_FOCUSED_WINDOW, so only the header
 * that is on screen answers; keys bound closer to the focus (POS, the table's own quick filter) still win.
 */
final class TableHeaderShortcuts {

    static final String NEW_KEY = "Ctrl+N";
    static final String REFRESH_KEY = "F5";
    static final String SEARCH_KEY = "Ctrl+F";
    private static final String REFRESH_NAME = "Actualizar dados";

    private TableHeaderShortcuts() {}

    static void install(JComponent header, ModernButton main, JComponent[] actions, JComponent filters) {
        AbstractButton refresh = findRefresh(actions);
        JTextField search = filters == null ? null : findSearch(filters);
        bind(header, "header.new", KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), () -> main);
        bind(header, "header.refresh", KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), () -> refresh);
        if (search != null) {
            header.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "header.search");
            header.getActionMap().put("header.search", new AbstractAction() {
                @Override public boolean isEnabled() { return search.isShowing() && search.isEnabled(); }
                @Override public void actionPerformed(ActionEvent e) {
                    search.requestFocusInWindow();
                    search.selectAll();
                }
            });
        }
        mention(main, NEW_KEY);
        mention(refresh, REFRESH_KEY);
        mention(search, SEARCH_KEY);
    }

    /**
     * Only enabled while its button can really act, so the key falls through to the next visible
     * header instead of being swallowed by a hidden or disabled one.
     */
    private static void bind(JComponent header, String name, KeyStroke key, Supplier<AbstractButton> target) {
        header.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, name);
        header.getActionMap().put(name, new AbstractAction() {
            @Override public boolean isEnabled() {
                AbstractButton button = target.get();
                return button != null && button.isShowing() && button.isEnabled();
            }
            @Override public void actionPerformed(ActionEvent e) {
                target.get().doClick();
            }
        });
    }

    private static void mention(JComponent component, String key) {
        if (component == null) return;
        String tip = component.getToolTipText();
        if (tip != null && tip.contains("(" + key + ")")) return;
        component.setToolTipText(tip == null || tip.isBlank() ? "Atalho: " + key : tip + " (" + key + ")");
    }

    static AbstractButton findRefresh(JComponent[] actions) {
        if (actions == null) return null;
        for (JComponent action : actions) {
            if (action instanceof AbstractButton button
                    && REFRESH_NAME.equals(button.getAccessibleContext().getAccessibleName())) return button;
        }
        return null;
    }

    /** The first free text field of the filters (not the editor of a combo box). */
    static JTextField findSearch(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof JComboBox<?>) continue;
            if (child instanceof JTextField field && SwingUtilities.getAncestorOfClass(JTable.class, field) == null) return field;
            if (child instanceof Container nested) {
                JTextField found = findSearch(nested);
                if (found != null) return found;
            }
        }
        return null;
    }
}
