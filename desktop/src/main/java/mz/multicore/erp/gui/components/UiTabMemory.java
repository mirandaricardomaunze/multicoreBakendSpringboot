package mz.multicore.erp.gui.components;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.event.HierarchyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Reopens each screen of the main window on the tab the user last used. Dialogs always open on their first
 * tab, and a tab chosen by the program before the screen appears (e.g. a shortcut to Transfers) wins.
 */
final class UiTabMemory {

    private static final Preferences PREFS = Preferences.userRoot().node("mz/multicore/erp/ui/tabs");
    private static final String STATE = "tabs.memory";

    private UiTabMemory() {}

    private static final class State {
        boolean restored;
        boolean chosenByProgram;
    }

    static void install(JTabbedPane tabs) {
        State state = new State();
        tabs.putClientProperty(STATE, state);
        tabs.addChangeListener(event -> {
            if (!state.restored) {
                if (tabs.getSelectedIndex() > 0) state.chosenByProgram = true;
            } else if (tabs.isShowing()) {
                save(key(tabs), TabTitles.selectedTitle(tabs));
            }
        });
        tabs.addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0 || !tabs.isShowing() || state.restored) return;
            state.restored = true;
            if (state.chosenByProgram || !(SwingUtilities.getWindowAncestor(tabs) instanceof JFrame)) return;
            int index = indexOf(tabs, read(key(tabs)));
            if (index > 0) tabs.setSelectedIndex(index);
        });
    }

    /** Identifies a tab strip by its tab names, so it survives restarts without each screen naming itself. */
    static String key(JTabbedPane tabs) {
        List<String> titles = new ArrayList<>();
        for (int i = 0; i < tabs.getTabCount(); i++) titles.add(TabTitles.normalize(TabTitles.titleAt(tabs, i)));
        return "t" + Integer.toHexString(String.join("|", titles).hashCode());
    }

    static int indexOf(JTabbedPane tabs, String title) {
        if (title == null) return -1;
        String wanted = TabTitles.normalize(title);
        for (int i = 0; i < tabs.getTabCount(); i++) {
            if (TabTitles.normalize(TabTitles.titleAt(tabs, i)).equals(wanted) && tabs.isEnabledAt(i)) return i;
        }
        return -1;
    }

    private static String read(String key) {
        try {
            return PREFS.get(key, null);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void save(String key, String title) {
        if (title == null) return;
        try {
            PREFS.put(key, title);
        } catch (RuntimeException ignored) {
            // Preferences unavailable: the screen still works, it just won't be remembered.
        }
    }
}
