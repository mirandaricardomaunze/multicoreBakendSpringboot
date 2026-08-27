package mz.multicore.erp.architecture.security;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

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
}
