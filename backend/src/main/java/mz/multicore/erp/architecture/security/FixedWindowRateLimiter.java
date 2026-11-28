package mz.multicore.erp.architecture.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;

/**
 * At most {@code limit} hits per key in a fixed window that opens with the key's first hit. Refused hits do not
 * count. In memory: one backend instance per store. Shared by the public API (per key) and the online shop (per IP).
 *
 * @param <K> what the limit is counted by (API key id, client IP, …)
 */
final class FixedWindowRateLimiter<K> {

    private static final int PRUNE_THRESHOLD = 10_000;

    private final int limit;
    private final Duration window;
    private final Clock clock;
    private final Map<K, Window> windows = new ConcurrentHashMap<>();

    FixedWindowRateLimiter(int limit, Duration window, Clock clock) {
        this.limit = limit;
        this.window = window;
        this.clock = clock;
    }

    /**
     * Counts one hit of the key.
     *
     * @return empty when the hit may go on; otherwise the seconds to send in {@code Retry-After} (≥ 1)
     */
    OptionalLong acquire(K key) {
        Instant now = clock.instant();
        if (windows.size() > PRUNE_THRESHOLD) {
            windows.values().removeIf(current -> current.expired(now));
        }
        Window current = windows.compute(key, (k, existing) ->
                existing == null || existing.expired(now) ? new Window(now) : existing);
        synchronized (current) {
            if (current.count < limit) {
                current.count++;
                return OptionalLong.empty();
            }
            long millisLeft = Duration.between(now, current.start.plus(window)).toMillis();
            return OptionalLong.of(Math.max(1L, (millisLeft + 999) / 1000));
        }
    }

    private final class Window {
        private final Instant start;
        private int count;

        private Window(Instant start) {
            this.start = start;
        }

        private boolean expired(Instant now) {
            return !now.isBefore(start.plus(window));
        }
    }
}
