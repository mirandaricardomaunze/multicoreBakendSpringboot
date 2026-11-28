package mz.multicore.erp.architecture.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.OptionalLong;

/**
 * Request limit of the public API (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.2, API-05): {@value #REQUESTS_PER_MINUTE}
 * requests per minute per API key, in a fixed one-minute window that opens with the key's first request.
 * Refused requests do not count. In memory, like {@link LoginRateLimiter}: one backend instance per store.
 */
@Component
public class PublicApiRateLimiter {

    public static final int REQUESTS_PER_MINUTE = 60;

    private final FixedWindowRateLimiter<Long> requests;

    public PublicApiRateLimiter() {
        this(Clock.systemUTC());
    }

    PublicApiRateLimiter(Clock clock) {
        this.requests = new FixedWindowRateLimiter<>(REQUESTS_PER_MINUTE, Duration.ofMinutes(1), clock);
    }

    /**
     * Counts one request of the key.
     *
     * @return empty when the request may go on; otherwise the seconds to send in {@code Retry-After} (≥ 1)
     */
    public OptionalLong acquire(long keyId) {
        return requests.acquire(keyId);
    }
}
