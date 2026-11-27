package mz.multicore.erp.modules.webhooks.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * The one place where the webhook retry schedule lives (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.3.2, WH-05): after the
 * 1st failure +1 min, 2nd +5 min, 3rd +30 min, 4th +2 h, 5th +12 h, counted from the end of the failed attempt; the
 * 6th failure is final ({@code FAILED}). Also the webhooks' clock, injectable for tests.
 */
@Component
public class WebhookRetryPolicy {

    /** Delay before the next attempt, indexed by the number of failures so far minus one. */
    static final List<Duration> DELAYS = List.of(Duration.ofMinutes(1), Duration.ofMinutes(5),
            Duration.ofMinutes(30), Duration.ofHours(2), Duration.ofHours(12));
    /** The initial attempt plus one per delay. */
    static final int MAX_ATTEMPTS = DELAYS.size() + 1;
    /** How long a claimed attempt is reserved; longer than the 10 s timeout of one attempt. */
    static final Duration CLAIM_LEASE = Duration.ofMinutes(2);

    private final Clock clock;

    public WebhookRetryPolicy() {
        this(Clock.systemDefaultZone());
    }

    WebhookRetryPolicy(Clock clock) {
        this.clock = clock;
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * When to try again after {@code failedAttempts} failed attempts, the last one ending at {@code attemptEnd};
     * empty when no attempt is left and the delivery becomes {@code FAILED}.
     */
    public Optional<LocalDateTime> nextAttemptAt(int failedAttempts, LocalDateTime attemptEnd) {
        if (failedAttempts < 1) {
            throw new IllegalArgumentException("failedAttempts must be at least 1");
        }
        if (failedAttempts >= MAX_ATTEMPTS) {
            return Optional.empty();
        }
        return Optional.of(attemptEnd.plus(DELAYS.get(failedAttempts - 1)));
    }

    /** Until when a claimed attempt stays reserved for the thread that claimed it. */
    public LocalDateTime claimLeaseUntil(LocalDateTime claimedAt) {
        return claimedAt.plus(CLAIM_LEASE);
    }
}
