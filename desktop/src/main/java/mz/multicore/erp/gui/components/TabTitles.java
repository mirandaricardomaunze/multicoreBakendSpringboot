package mz.multicore.erp.gui.components;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

/** Reads tab names so the top bar path and the table headers can tell when a title is already on screen. */
public final class TabTitles {

    private TabTitles() {}

    /** The outermost visible tab pane of a screen, or null when the screen has no tabs. */
    public static JTabbedPane firstTabs(Component root) {
        Deque<Component> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            Component current = queue.poll();
            if (!current.isVisible()) continue;
            if (current instanceof JTabbedPane tabs) return tabs;
            if (current instanceof Container container) {
                for (Component child : container.getComponents()) queue.add(child);
            }
        }
        return null;
    }

    /** Name of the selected tab, or null. */
    public static String selectedTitle(JTabbedPane tabs) {
        int index = tabs.getSelectedIndex();
        return index < 0 ? null : titleAt(tabs, index);
    }

    static String titleAt(JTabbedPane tabs, int index) {
        String title = tabs.getTitleAt(index);
        if (title != null && !title.isBlank()) return title.trim();
        Component custom = tabs.getTabComponentAt(index);
        return custom == null ? null : firstLabelText(custom);
    }

    /**
     * True when an enclosing tab already says what the title says: the title contains the tab name
     * ("Recibos Emitidos (Liquidações)" in "Recibos", "Central de Pedidos e Separação" in
     * "Pedidos & Separação") or the tab name starts with the title. When the tab page holds more than one
     * table header the titles are what tells the tables apart, so they all stay.
     */
    static boolean repeatsTab(Component component, String title) {
        String wanted = words(title);
        if (wanted.isBlank()) return false;
        Component child = component;
        for (Container parent = component.getParent(); parent != null; child = parent, parent = parent.getParent()) {
            if (parent instanceof JTabbedPane tabs) {
                int index = tabs.indexOfComponent(child);
                if (index < 0) continue;
                String tab = words(titleAt(tabs, index));
                boolean said = !tab.isBlank() && (wanted.contains(tab) || tab.startsWith(wanted));
                if (said && countHeadings(child) <= 1) return true;
            }
        }
        return false;
    }

    /** Normalised words padded with spaces, so "contains" only matches whole words; "&" reads as "e". */
    static String words(String text) {
        String plain = normalize(text).replace("&", " e ").replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
        return plain.isEmpty() ? "" : " " + plain + " ";
    }

    private static int countHeadings(Component root) {
        int count = root instanceof JComponent c && c.getClientProperty(TableCardTop.HEADING) != null ? 1 : 0;
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) count += countHeadings(child);
        }
        return count;
    }

    /** Lower case, without counters such as "(3)", so "Alertas (3)" matches "Alertas". */
    static String normalize(String text) {
        if (text == null) return "";
        return text.replaceAll("\\(\\s*\\d+\\s*\\)", " ").replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    private static String firstLabelText(Component component) {
        if (component instanceof JLabel label && label.getText() != null && !label.getText().isBlank()) return label.getText().trim();
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                String text = firstLabelText(child);
                if (text != null) return text;
            }
        }
        return null;
    }
}
