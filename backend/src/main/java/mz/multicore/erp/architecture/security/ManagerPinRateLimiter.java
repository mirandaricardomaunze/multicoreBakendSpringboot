package mz.multicore.erp.architecture.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Trava de segurança activa contra força bruta na verificação de PIN de gerente/supervisor (4 dígitos).
 *
 * <p>Após {@code maxAttempts} (padrão: 3) tentativas falhadas consecutivas para uma empresa/origem,
 * bloqueia novas tentativas durante {@code lockoutMinutes} (padrão: 5 minutos).
 * Um PIN correto limpa imediatamente o contador de falhas.
 */
@Component
public class ManagerPinRateLimiter {

    private final int maxAttempts;
    private final Duration lockout;
    private final Clock clock;
    private final Map<String, PinAttempt> attempts = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    public ManagerPinRateLimiter(
            @Value("${security.pin.max-attempts:3}") int maxAttempts,
            @Value("${security.pin.lockout-minutes:5}") int lockoutMinutes
    ) {
        this(maxAttempts, lockoutMinutes, Clock.systemUTC());
    }

    public ManagerPinRateLimiter(int maxAttempts, int lockoutMinutes, Clock clock) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.lockout = Duration.ofMinutes(Math.max(1, lockoutMinutes));
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    /**
     * Verifica se o contexto de empresa e origem IP está atualmente bloqueado.
     * Retorna os minutos restantes de bloqueio ou 0 se permitido.
     */
    public long getRemainingLockMinutes(Long companyId, String clientIp) {
        PinAttempt attempt = attempts.get(buildKey(companyId, clientIp));
        if (attempt != null && attempt.lockedUntil != null) {
            Instant now = clock.instant();
            if (now.isBefore(attempt.lockedUntil)) {
                return Duration.between(now, attempt.lockedUntil).toMinutes() + 1;
            }
        }
        return 0;
    }

    /**
     * Retorna true se estiver sob bloqueio ativo.
     */
    public boolean isLocked(Long companyId, String clientIp) {
        return getRemainingLockMinutes(companyId, clientIp) > 0;
    }

    /**
     * Regista uma tentativa de PIN falhada. Ao atingir o limite, ativa o bloqueio.
     */
    public void recordFailure(Long companyId, String clientIp) {
        attempts.compute(buildKey(companyId, clientIp), (k, attempt) -> {
            Instant now = clock.instant();
            if (attempt == null || (attempt.lockedUntil != null && now.isAfter(attempt.lockedUntil))) {
                attempt = new PinAttempt();
            }
            attempt.count++;
            if (attempt.count >= maxAttempts) {
                attempt.lockedUntil = now.plus(lockout);
            }
            return attempt;
        });
    }

    /**
     * PIN correto: limpa o contador e cancela qualquer bloqueio prévio.
     */
    public void recordSuccess(Long companyId, String clientIp) {
        attempts.remove(buildKey(companyId, clientIp));
    }

    private static String buildKey(Long companyId, String clientIp) {
        String comp = companyId != null ? companyId.toString() : "global";
        String ip = (clientIp != null && !clientIp.isBlank()) ? clientIp.trim() : "local";
        return "pin:" + comp + ":" + ip;
    }

    private static final class PinAttempt {
        int count;
        Instant lockedUntil;
    }
}
