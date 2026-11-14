package mz.multicore.erp.gui.components;

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

    /** True when an enclosing tab already says it: same name, or the tab name starts with the title. */
    static boolean repeatsTab(Component component, String title) {
        String wanted = normalize(title);
        if (wanted.isEmpty()) return false;
        Component child = component;
        for (Container parent = component.getParent(); parent != null; child = parent, parent = parent.getParent()) {
            if (parent instanceof JTabbedPane tabs) {
                int index = tabs.indexOfComponent(child);
                String tab = index < 0 ? "" : normalize(titleAt(tabs, index));
                if (!tab.isEmpty() && (tab.equals(wanted) || tab.startsWith(wanted + " "))) return true;
            }
        }
        return false;
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
