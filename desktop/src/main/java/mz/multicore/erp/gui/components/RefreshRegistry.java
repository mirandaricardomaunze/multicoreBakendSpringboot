package mz.multicore.erp.gui.components;

import javax.swing.AbstractButton;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Every "Actualizar" button built by {@link UIHelper#createRefreshButton(Runnable)}, held weakly.
 *
 * <p>Auto-refresh presses the ones on screen, so a background change reloads exactly what a click
 * would, with the screen's own filters (docs/ACTUALIZACAO_AUTOMATICA_SPEC.md §4.2).
 */
public final class RefreshRegistry {

    private static final List<WeakReference<AbstractButton>> BUTTONS = new ArrayList<>();

    private RefreshRegistry() {
    }

    static synchronized void register(AbstractButton button) {
        BUTTONS.removeIf(ref -> ref.get() == null);
        BUTTONS.add(new WeakReference<>(button));
    }

    /** Live registered buttons that pass {@code visible}; garbage-collected ones are dropped. */
    public static synchronized List<AbstractButton> matching(Predicate<AbstractButton> visible) {
        List<AbstractButton> result = new ArrayList<>();
        BUTTONS.removeIf(ref -> ref.get() == null);
        for (WeakReference<AbstractButton> ref : BUTTONS) {
            AbstractButton button = ref.get();
            if (button != null && visible.test(button)) result.add(button);
        }
        return result;
    }

    /** On screen right now, enabled and inside {@code window}. */
    public static List<AbstractButton> onScreen(java.awt.Window window) {
        return matching(button -> button.isShowing() && button.isEnabled()
                && javax.swing.SwingUtilities.getWindowAncestor(button) == window);
    }
}
