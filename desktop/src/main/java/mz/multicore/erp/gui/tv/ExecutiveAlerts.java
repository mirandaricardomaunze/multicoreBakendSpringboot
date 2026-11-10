package mz.multicore.erp.gui.tv;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Exception alert shown on the Painel Executivo footer (SA-C21, {@code docs/ANTIFRAUDE_SECTORES_SPEC.md}).
 * The anti-fraud module registers where the counts come from; the panel never depends on that module. Counts are
 * read in the background and cached for a minute; the footer, rebuilt on every automatic refresh, shows the latest.
 * Without a registered source, or if the user may not read the report, nothing is shown.
 */
public final class ExecutiveAlerts {

    /** High and medium findings of the last days. */
    public record Counts(long high, long medium) {
        public boolean any() {
            return high + medium > 0;
        }
    }

    private static final long TTL_MS = 60_000;
    private static volatile Supplier<Counts> source;
    private static volatile Counts latest;
    private static volatile long readAt;
    private static final AtomicBoolean reading = new AtomicBoolean();

    private ExecutiveAlerts() {
    }

    public static void register(Supplier<Counts> countsSource) {
        source = countsSource;
        latest = null;
        readAt = 0;
    }

    /** Latest counts, starting a background read when they are older than a minute. May be {@code null}. */
    public static Counts current() {
        Supplier<Counts> s = source;
        if (s != null && System.currentTimeMillis() - readAt > TTL_MS && reading.compareAndSet(false, true)) {
            Thread worker = new Thread(() -> {
                try {
                    latest = s.get();
                } catch (RuntimeException unavailable) {
                    latest = null; // no permission or no server: the panel simply shows no alert
                } finally {
                    readAt = System.currentTimeMillis();
                    reading.set(false);
                }
            }, "executive-alerts");
            worker.setDaemon(true);
            worker.start();
        }
        return latest;
    }

    /** Footer text, or {@code null} when there is nothing to warn about. */
    static String text(Counts counts) {
        if (counts == null || !counts.any()) return null;
        return "Excepções a verificar (7 dias): " + counts.high() + (counts.high() == 1 ? " alta" : " altas") + " · "
                + counts.medium() + (counts.medium() == 1 ? " média" : " médias") + "  —  módulo Excepções";
    }

    /** Test hook: sets the cached counts directly. */
    static void setForTest(Counts counts) {
        latest = counts;
        readAt = System.currentTimeMillis();
    }
}
