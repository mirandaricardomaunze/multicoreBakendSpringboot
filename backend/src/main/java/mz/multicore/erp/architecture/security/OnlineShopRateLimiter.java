package mz.multicore.erp.architecture.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.OptionalLong;

/**
 * Limits of the public online shop per client IP (docs/LOJA_ONLINE_SPEC.md §3.2, LO-04):
 * {@value #REQUESTS_PER_MINUTE} requests per minute to {@code /api/shop/**} and {@value #ORDERS_PER_HOUR} order
 * attempts per hour. Every order attempt counts, accepted or refused by the checkout rules, so a robot cannot probe
 * the checkout without limit.
 */
@Component
public class OnlineShopRateLimiter {

    public static final int REQUESTS_PER_MINUTE = 60;
    public static final int ORDERS_PER_HOUR = 5;

    private final FixedWindowRateLimiter<String> requests;
    private final FixedWindowRateLimiter<String> orders;

    public OnlineShopRateLimiter() {
        this(Clock.systemUTC());
    }

    OnlineShopRateLimiter(Clock clock) {
        this.requests = new FixedWindowRateLimiter<>(REQUESTS_PER_MINUTE, Duration.ofMinutes(1), clock);
        this.orders = new FixedWindowRateLimiter<>(ORDERS_PER_HOUR, Duration.ofHours(1), clock);
    }

    /** One request from the IP; empty when it may go on, otherwise the {@code Retry-After} seconds. */
    public OptionalLong acquireRequest(String clientIp) {
        return requests.acquire(clientIp);
    }

    /** One order attempt from the IP; empty when it may go on, otherwise the {@code Retry-After} seconds. */
    public OptionalLong acquireOrder(String clientIp) {
        return orders.acquire(clientIp);
    }
}
