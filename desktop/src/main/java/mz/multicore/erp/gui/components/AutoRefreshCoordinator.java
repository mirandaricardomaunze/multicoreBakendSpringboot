package mz.multicore.erp.gui.components;

import mz.multicore.erp.architecture.changes.ChangeVersionDTO;

import javax.swing.AbstractButton;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Reloads the visible screen when another station changes data (docs/ACTUALIZACAO_AUTOMATICA_SPEC.md §4).
 *
 * <p>The poll runs on a daemon thread every {@link AutoRefreshPolicy#POLL_INTERVAL_MS}; the
 * reload itself presses the on-screen "Actualizar" buttons on the EDT, only when the user is not
 * busy, and puts back each visible table's page and selected row afterwards.
 */
public final class AutoRefreshCoordinator {

    private static volatile long lastKeyAt;

    static {
        try {
            Toolkit.getDefaultToolkit().addAWTEventListener(
                    event -> lastKeyAt = System.currentTimeMillis(), AWTEvent.KEY_EVENT_MASK);
        } catch (RuntimeException ignored) {
            // Headless or restricted toolkit: typing detection simply stays off.
        }
    }

    private final JFrame frame;
    private final Supplier<ChangeVersionDTO> versionSource;
    private final Supplier<Long> companySource;
    private final Runnable afterRefresh;
    private final ScheduledExecutorService executor;

    private ChangeVersionDTO last;
    private Long lastCompany;
    private volatile boolean pending;

    private AutoRefreshCoordinator(JFrame frame, Supplier<ChangeVersionDTO> versionSource,
                                   Supplier<Long> companySource, Runnable afterRefresh) {
        this.frame = frame;
        this.versionSource = versionSource;
        this.companySource = companySource;
        this.afterRefresh = afterRefresh;
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "auto-refresh");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Starts polling for {@code frame} and stops by itself when the frame is closed (disposed).
     * A failing {@code companySource} (no session yet) counts as "no company": nothing is polled.
     */
    public static AutoRefreshCoordinator start(JFrame frame, Supplier<ChangeVersionDTO> versionSource,
                                               Supplier<Long> companySource, Runnable afterRefresh) {
        AutoRefreshCoordinator coordinator = new AutoRefreshCoordinator(frame, versionSource, companySource, afterRefresh);
        long interval = AutoRefreshPolicy.POLL_INTERVAL_MS;
        coordinator.executor.scheduleWithFixedDelay(coordinator::poll, interval, interval, TimeUnit.MILLISECONDS);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent event) { coordinator.stop(); }
        });
        return coordinator;
    }

    public void stop() {
        executor.shutdownNow();
    }

    private void poll() {
        try {
            Long company = companySource.get();
            if (company == null) return;
            ChangeVersionDTO now = versionSource.get();
            if (now == null) return;
            if (!Objects.equals(company, lastCompany)) {
                // A company switch already reloads the screen; this reading is only the new baseline.
                lastCompany = company;
                last = now;
                pending = false;
                return;
            }
            if (AutoRefreshPolicy.changed(last, now)) pending = true;
            last = now;
            if (pending) SwingUtilities.invokeLater(this::refreshIfIdle);
        } catch (RuntimeException ignored) {
            // Network hiccup or server restarting: try again on the next round, never bother the user.
        }
    }

    private void refreshIfIdle() {
        if (!pending || !frame.isDisplayable()) return;
        List<JTable> tables = visibleTables(frame.getContentPane());
        AutoRefreshPolicy.UiState state = new AutoRefreshPolicy.UiState(
                modalOpen(),
                System.currentTimeMillis() - lastKeyAt,
                tables.stream().anyMatch(JTable::isEditing),
                (frame.getExtendedState() & Frame.ICONIFIED) != 0,
                UiAsyncTaskSupport.activeLoads(frame) > 0);
        if (!AutoRefreshPolicy.canRefresh(state)) return;
        pending = false;
        for (JTable table : tables) {
            TableStateKeeper.restoreAfterReload(table, TableStateKeeper.capture(table));
        }
        for (AbstractButton button : RefreshRegistry.onScreen(frame)) {
            button.doClick(0);
        }
        if (afterRefresh != null) afterRefresh.run();
    }

    private boolean modalOpen() {
        for (Window window : Window.getWindows()) {
            if (window instanceof Dialog dialog && dialog.isShowing() && dialog.isModal()) return true;
        }
        return false;
    }

    private static List<JTable> visibleTables(Container root) {
        List<JTable> tables = new ArrayList<>();
        collect(root, tables);
        return tables;
    }

    private static void collect(Container container, List<JTable> tables) {
        for (Component child : container.getComponents()) {
            if (!child.isShowing()) continue;
            if (child instanceof JTable table) tables.add(table);
            if (child instanceof Container nested) collect(nested, tables);
        }
    }
}
