package mz.multicore.erp.architecture.security;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Testes do rate-limiter de login (RL-01..04) — lógica pura, sem Spring. */
class LoginRateLimiterTest {

    @Test
    void locksAfterMaxFailures() {
        LoginRateLimiter rl = new LoginRateLimiter(3, 15);
        assertDoesNotThrow(() -> rl.checkAllowed("bob"));
        rl.recordFailure("bob");
        rl.recordFailure("bob");
        rl.recordFailure("bob"); // 3ª falha → bloqueado
        assertThrows(BusinessRuleException.class, () -> rl.checkAllowed("bob"));
    }

    @Test
    void successResetsCounter() {
        LoginRateLimiter rl = new LoginRateLimiter(3, 15);
        rl.recordFailure("bob");
        rl.recordFailure("bob");
        rl.recordSuccess("bob"); // limpa
        rl.recordFailure("bob");
        rl.recordFailure("bob"); // 2 falhas após reset < 3
        assertDoesNotThrow(() -> rl.checkAllowed("bob"));
    }

    @Test
    void otherUserUnaffected() {
        LoginRateLimiter rl = new LoginRateLimiter(2, 15);
        rl.recordFailure("bob");
        rl.recordFailure("bob"); // bob bloqueado
        assertThrows(BusinessRuleException.class, () -> rl.checkAllowed("bob"));
        assertDoesNotThrow(() -> rl.checkAllowed("alice"));
    }

    @Test
    void keyIsCaseAndSpaceInsensitive() {
        LoginRateLimiter rl = new LoginRateLimiter(2, 15);
        rl.recordFailure("Bob");
        rl.recordFailure(" bob ");
        assertThrows(BusinessRuleException.class, () -> rl.checkAllowed("BOB"));
    }

    @Test
    void failuresFromOneOriginDoNotLockSameUserElsewhere() {
        LoginRateLimiter rl = new LoginRateLimiter(3, 15, 30, Clock.systemUTC());
        for (int i = 0; i < 3; i++) rl.recordFailure("bob", "203.0.113.1");
        assertThrows(BusinessRuleException.class, () -> rl.checkAllowed("bob", "203.0.113.1"));
        assertDoesNotThrow(() -> rl.checkAllowed("bob", "203.0.113.2"));
    }

    @Test
    void arbitraryUsernamesCannotGrowAttemptStoreWithoutBound() {
        LoginRateLimiter rl = new LoginRateLimiter(3, 15, 30, Clock.systemUTC(), 5);
        for (int i = 0; i < 20; i++) rl.recordFailure("unknown-" + i, "203.0.113.1");
        org.junit.jupiter.api.Assertions.assertTrue(rl.trackedKeys() <= 5);
    }
}
