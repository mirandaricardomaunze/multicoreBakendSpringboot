package mz.multicore.erp.gui.components;

import mz.multicore.erp.architecture.changes.ChangeVersionDTO;

import java.util.Objects;

/**
 * Pure decisions of auto-refresh: did data change, and may the screen reload now.
 * See docs/ACTUALIZACAO_AUTOMATICA_SPEC.md §4.1 and §4.3.
 */
public final class AutoRefreshPolicy {

    public static final long POLL_INTERVAL_MS = 5_000;
    public static final long TYPING_QUIET_MS = 3_000;

    private AutoRefreshPolicy() {
    }

    /** What the user is doing right now; any busy flag postpones the reload. */
    public record UiState(boolean modalOpen, long millisSinceLastKey, boolean editingCell,
                          boolean minimized, boolean loading) {
    }

    /**
     * A first reading ({@code last == null}) only records the baseline. A new epoch (server
     * restarted) or a higher version means somebody changed data.
     */
    public static boolean changed(ChangeVersionDTO last, ChangeVersionDTO now) {
        if (last == null || now == null) return false;
        if (!Objects.equals(last.epoch(), now.epoch())) return true;
        return now.version() > last.version();
    }

    public static boolean canRefresh(UiState state) {
        return !state.modalOpen()
                && state.millisSinceLastKey() >= TYPING_QUIET_MS
                && !state.editingCell()
                && !state.minimized()
                && !state.loading();
    }
}
