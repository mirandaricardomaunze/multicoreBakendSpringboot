package com.phcpro.architecture.security;

import com.phcpro.architecture.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Trava força-bruta no login: após {@code security.login.max-attempts} falhas seguidas para um
 * utilizador, bloqueia-o durante {@code security.login.lockout-minutes}. Um login com sucesso limpa o
 * contador. Estado em memória (adequado ao backend em processo do desktop). Ver
 * {@code docs/SEGURANCA_RATE_LIMIT_SPEC.md}.
 */
@Component
public class LoginRateLimiter {

    private final int maxAttempts;
    private final Duration lockout;
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public LoginRateLimiter(@Value("${security.login.max-attempts:5}") int maxAttempts,
                            @Value("${security.login.lockout-minutes:15}") int lockoutMinutes) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.lockout = Duration.ofMinutes(Math.max(1, lockoutMinutes));
    }

    /** Lança se o utilizador estiver bloqueado por excesso de tentativas falhadas. */
    public void checkAllowed(String username) {
        Attempt a = attempts.get(key(username));
        if (a != null && a.lockedUntil != null && Instant.now().isBefore(a.lockedUntil)) {
            long mins = Duration.between(Instant.now(), a.lockedUntil).toMinutes() + 1;
            throw new BusinessRuleException(
                    "Demasiadas tentativas falhadas. Tente novamente em " + mins + " minuto(s).");
        }
    }

    /** Regista uma tentativa falhada; ao atingir o limite, bloqueia. */
    public void recordFailure(String username) {
        attempts.compute(key(username), (k, a) -> {
            Instant now = Instant.now();
            if (a == null || (a.lockedUntil != null && now.isAfter(a.lockedUntil))) {
                a = new Attempt(); // janela nova após expirar o bloqueio
            }
            a.count++;
            if (a.count >= maxAttempts) {
                a.lockedUntil = now.plus(lockout);
            }
            return a;
        });
    }

    /** Login com sucesso → limpa o contador do utilizador. */
    public void recordSuccess(String username) {
        attempts.remove(key(username));
    }

    private static String key(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    private static final class Attempt {
        int count;
        Instant lockedUntil;
    }
}
