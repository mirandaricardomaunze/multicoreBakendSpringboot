package mz.multicore.erp.gui.sector;

import javax.swing.Icon;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Active activity capabilities of the company in use (SA-C01, docs/SECTORES_ACTIVIDADE_SPEC.md).
 *
 * <p>Features that live inside other screens (a button, a tab, a shortcut) bind to a capability here, so the
 * activity profile shows or hides them like it does for sector menus. Hiding never deletes data. While no
 * capability set is known (not loaded yet, or no profile) every feature stays available, as before SA-C01.
 */
public final class ActivityCapabilities {

    public static final String MULTI_UNIT_SALES = "MULTI_UNIT_SALES";
    public static final String WEIGHED_SALES = "WEIGHED_SALES";
    public static final String MARKDOWNS = "MARKDOWNS";
    public static final String DELIVERIES = "DELIVERIES";
    public static final String BULK_STOCK = "BULK_STOCK";

    private static volatile Set<String> active = Set.of();
    private static final List<WeakReference<Runnable>> listeners = new CopyOnWriteArrayList<>();

    private ActivityCapabilities() {
    }

    /** Replaces the active set (after login, company switch or a profile save) and refreshes bound features. */
    public static void apply(Collection<String> capabilities) {
        active = capabilities == null ? Set.of() : Set.copyOf(new HashSet<>(capabilities));
        Runnable notify = () -> {
            listeners.removeIf(ref -> ref.get() == null);
            listeners.forEach(ref -> {
                Runnable listener = ref.get();
                if (listener != null) listener.run();
            });
        };
        if (SwingUtilities.isEventDispatchThread()) notify.run(); else SwingUtilities.invokeLater(notify);
    }

    public static boolean enabled(String capability) {
        Set<String> current = active;
        return current.isEmpty() || current.contains(capability);
    }

    /** Shows the component only while the capability is active. */
    public static void bindVisible(Component component, String capability) {
        Runnable update = () -> {
            component.setVisible(enabled(capability));
            if (component.getParent() != null) {
                component.getParent().revalidate();
                component.getParent().repaint();
            }
        };
        update.run();
        keep(component, update);
    }

    /** Keeps a tab in the pane only while the capability is active; it returns at the same title and icon. */
    public static void bindTab(JTabbedPane tabs, Component content, String title, Icon icon, String tooltip, String capability) {
        Runnable update = () -> {
            int index = tabs.indexOfComponent(content);
            boolean on = enabled(capability);
            if (on && index < 0) {
                tabs.addTab(title, icon, content, tooltip);
            } else if (!on && index >= 0) {
                if (tabs.getSelectedIndex() == index && tabs.getTabCount() > 1) tabs.setSelectedIndex(0);
                tabs.removeTabAt(index);
            }
        };
        update.run();
        keep(tabs, update);
    }

    /** Runs the action now and after every capability change, for as long as {@code owner} is alive. */
    public static void onChange(Object owner, Runnable action) {
        action.run();
        keep(owner, action);
    }

    /**
     * The listener is held strongly by its owner (a client property or the owner itself) and weakly here, so a
     * closed screen does not leak through this static registry.
     */
    private static void keep(Object owner, Runnable listener) {
        if (owner instanceof javax.swing.JComponent c) {
            @SuppressWarnings("unchecked")
            List<Runnable> held = (List<Runnable>) c.getClientProperty(ActivityCapabilities.class);
            if (held == null) {
                held = new CopyOnWriteArrayList<>();
                c.putClientProperty(ActivityCapabilities.class, held);
            }
            held.add(listener);
        } else {
            STRONG_FALLBACK.add(listener);
        }
        listeners.add(new WeakReference<>(listener));
    }

    /** Owners that are not Swing components keep their listener here for the app's lifetime. */
    private static final List<Runnable> STRONG_FALLBACK = new CopyOnWriteArrayList<>();

    /** Test hook: forget the set and every listener. */
    static void resetForTests() {
        active = Set.of();
        listeners.clear();
        STRONG_FALLBACK.clear();
    }
}
