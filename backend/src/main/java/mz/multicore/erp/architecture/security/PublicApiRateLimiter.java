package mz.multicore.erp.architecture.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Request limit of the public API (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.2, API-05): {@value #REQUESTS_PER_MINUTE}
 * requests per minute per API key, in a fixed one-minute window that opens with the key's first request.
 * Refused requests do not count. In memory, like {@link LoginRateLimiter}: one backend instance per store.
 */
@Component
public class PublicApiRateLimiter {

    public static final int REQUESTS_PER_MINUTE = 60;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final int PRUNE_THRESHOLD = 10_000;

    private final Clock clock;
    private final Map<Long, Window> windows = new ConcurrentHashMap<>();

    public PublicApiRateLimiter() {
        this(Clock.systemUTC());
    }

    PublicApiRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /**
     * Counts one request of the key.
     *
     * @return empty when the request may go on; otherwise the seconds to send in {@code Retry-After} (≥ 1)
     */
    public OptionalLong acquire(long keyId) {
        Instant now = clock.instant();
        if (windows.size() > PRUNE_THRESHOLD) {
            windows.values().removeIf(window -> window.expired(now));
        }
        Window window = windows.compute(keyId, (id, current) ->
                current == null || current.expired(now) ? new Window(now) : current);
        synchronized (window) {
            if (window.count < REQUESTS_PER_MINUTE) {
                window.count++;
                return OptionalLong.empty();
            }
            long millisLeft = Duration.between(now, window.start.plus(WINDOW)).toMillis();
            return OptionalLong.of(Math.max(1L, (millisLeft + 999) / 1000));
        }
    }

    private static final class Window {
        private final Instant start;
        private int count;

        private Window(Instant start) {
            this.start = start;
        }

        private boolean expired(Instant now) {
            return !now.isBefore(start.plus(WINDOW));
        }
    }
}
