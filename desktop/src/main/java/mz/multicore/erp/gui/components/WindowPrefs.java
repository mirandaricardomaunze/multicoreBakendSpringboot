package mz.multicore.erp.gui.components;

import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.util.prefs.Preferences;

/**
 * Remembers the size, position and maximized state of the main window between sessions
 * (docs/MEMORIA_ECRA_SPEC.md). A saved position that no longer fits any screen (monitor unplugged) is
 * ignored and the window opens maximized.
 */
public final class WindowPrefs {

    private static Preferences store = Preferences.userRoot().node("mz/multicore/erp/ui/window");

    private WindowPrefs() {
    }

    static void useStore(Preferences preferences) {
        store = preferences;
    }

    public static void save(Frame frame) {
        if (frame == null) return;
        boolean maximized = (frame.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
        store.putBoolean("maximized", maximized);
        if (!maximized) {
            Rectangle b = frame.getBounds();
            store.put("bounds", b.x + "," + b.y + "," + b.width + "," + b.height);
        }
    }

    /** Saved normal bounds, or null when there are none or they fall outside every screen. */
    public static Rectangle savedBounds() {
        String raw = store.get("bounds", null);
        if (raw == null) return null;
        String[] p = raw.split(",");
        if (p.length != 4) return null;
        try {
            Rectangle r = new Rectangle(Integer.parseInt(p[0]), Integer.parseInt(p[1]),
                    Integer.parseInt(p[2]), Integer.parseInt(p[3]));
            if (r.width < 640 || r.height < 480) return null;
            return fitsAScreen(r) ? r : null;
        } catch (NumberFormatException corrupt) {
            return null;
        }
    }

    /** Maximized unless the operator left the window in normal size with valid bounds. */
    public static int savedState() {
        return store.getBoolean("maximized", true) || savedBounds() == null ? Frame.MAXIMIZED_BOTH : Frame.NORMAL;
    }

    static boolean fitsAScreen(Rectangle r) {
        if (GraphicsEnvironment.isHeadless()) return true;
        for (var device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            Rectangle screen = device.getDefaultConfiguration().getBounds();
            Rectangle visible = screen.intersection(r);
            if (visible.width >= 200 && visible.height >= 120) return true;
        }
        return false;
    }
}
