package mz.multicore.erp.gui.components;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.util.prefs.Preferences;

/**
 * Whether the side menu opens collapsed. The user's own choice wins; without one, small screens
 * (under {@value #SMALL_SCREEN_WIDTH} px wide) start with icons only so tables keep their columns.
 */
public final class SidebarCollapsePreference {

    public static final int SMALL_SCREEN_WIDTH = 1440;
    private static final String KEY = "sidebar.collapsed";
    private static final Preferences PREFS = Preferences.userRoot().node("mz/multicore/erp/ui");

    private SidebarCollapsePreference() {}

    public static boolean startCollapsed() {
        return decide(read(), screenWidth());
    }

    /** Called only for the user's own toggles (button, Ctrl+B), never for automatic ones such as the POS. */
    public static void remember(boolean collapsed) {
        try {
            PREFS.putBoolean(KEY, collapsed);
        } catch (RuntimeException ignored) {
            // Preferences unavailable: the menu still works, it just won't be remembered.
        }
    }

    static boolean decide(String saved, int screenWidth) {
        if (saved != null) return Boolean.parseBoolean(saved);
        return screenWidth > 0 && screenWidth < SMALL_SCREEN_WIDTH;
    }

    private static String read() {
        try {
            return PREFS.get(KEY, null);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static int screenWidth() {
        return GraphicsEnvironment.isHeadless() ? 0 : Toolkit.getDefaultToolkit().getScreenSize().width;
    }
}
