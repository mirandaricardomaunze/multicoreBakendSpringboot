package mz.multicore.erp.architecture.security;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Trava força-bruta no login:
 * 1. Nível Utilizador: após {@code maxAttempts} (padrão 5) falhas seguidas para um utilizador,
 *    bloqueia-o durante {@code lockoutMinutes} (padrão 15).
 * 2. Nível IP de Origem: após {@code maxIpAttempts} (padrão 30) falhas a partir do mesmo IP,
 *    bloqueia o IP durante {@code lockoutMinutes}.
 * Um login com sucesso limpa o contador do utilizador e decrementa/limpa o do IP.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_TRACKED_KEYS = 10_000;

    private final int maxAttempts;
    private final int maxIpAttempts;
    private final Duration lockout;
    private final Clock clock;
    private final int maxTrackedKeys;
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    public LoginRateLimiter(
            @Value("${security.login.max-attempts:5}") int maxAttempts,
            @Value("${security.login.lockout-minutes:15}") int lockoutMinutes,
            @Value("${security.login.max-ip-attempts:30}") int maxIpAttempts
    ) {
        this(maxAttempts, lockoutMinutes, maxIpAttempts, Clock.systemUTC());
    }

    public LoginRateLimiter(int maxAttempts, int lockoutMinutes) {
        this(maxAttempts, lockoutMinutes, 30, Clock.systemUTC());
    }

    public LoginRateLimiter(int maxAttempts, int lockoutMinutes, int maxIpAttempts, Clock clock) {
        this(maxAttempts, lockoutMinutes, maxIpAttempts, clock, MAX_TRACKED_KEYS);
    }

    LoginRateLimiter(int maxAttempts, int lockoutMinutes, int maxIpAttempts, Clock clock,
                     int maxTrackedKeys) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.lockout = Duration.ofMinutes(Math.max(1, lockoutMinutes));
        this.maxIpAttempts = Math.max(5, maxIpAttempts);
        this.clock = clock != null ? clock : Clock.systemUTC();
        this.maxTrackedKeys = Math.max(2, maxTrackedKeys);
    }

    /** Lança se o utilizador ou o IP estiverem bloqueados por excesso de tentativas falhadas. */
    public void checkAllowed(String username, String clientIp) {
        Instant now = clock.instant();

        // 1. Verificação ao nível de IP
        if (clientIp != null && !clientIp.isBlank()) {
            Attempt ipAttempt = attempts.get(ipKey(clientIp));
            if (ipAttempt != null && ipAttempt.lockedUntil != null && now.isBefore(ipAttempt.lockedUntil)) {
                long mins = Duration.between(now, ipAttempt.lockedUntil).toMinutes() + 1;
                throw new BusinessRuleException(
                        "Demasiadas tentativas de autenticação a partir deste endereço IP. Tente novamente em " + mins + " minuto(s).");
            }
        }

        // 2. Verificação ao nível de Utilizador
        Attempt userAttempt = attempts.get(userKey(username, clientIp));
        if (userAttempt != null && userAttempt.lockedUntil != null && now.isBefore(userAttempt.lockedUntil)) {
            long mins = Duration.between(now, userAttempt.lockedUntil).toMinutes() + 1;
            throw new BusinessRuleException(
                    "Demasiadas tentativas falhadas. Tente novamente em " + mins + " minuto(s).");
        }
    }

    /** Retrocompatível: verificação apenas por username. */
    public void checkAllowed(String username) {
        checkAllowed(username, null);
    }

    /** Regista uma tentativa falhada para o utilizador e para o IP. Ao atingir o limite, bloqueia. */
    public synchronized void recordFailure(String username, String clientIp) {
        Instant now = clock.instant();
        prune(now);

        // O limite por utilizador e origem nao permite a uma origem bloquear outra.
        attempts.compute(userKey(username, clientIp), (k, a) -> {
            if (a == null || !now.isBefore(a.lastSeen.plus(lockout))) {
                a = new Attempt();
            }
            a.count++;
            a.lastSeen = now;
            if (a.count >= maxAttempts) {
                a.lockedUntil = now.plus(lockout);
            }
            return a;
        });

        // Falha no IP
        if (clientIp != null && !clientIp.isBlank()) {
            attempts.compute(ipKey(clientIp), (k, a) -> {
                if (a == null || !now.isBefore(a.lastSeen.plus(lockout))) {
                    a = new Attempt();
                }
                a.count++;
                a.lastSeen = now;
                if (a.count >= maxIpAttempts) {
                    a.lockedUntil = now.plus(lockout);
                }
                return a;
            });
        }
        prune(now);
    }

    /** Retrocompatível: registo de falha apenas por username. */
    public void recordFailure(String username) {
        recordFailure(username, null);
    }

    /** Login com sucesso → limpa o contador do utilizador e do IP. */
    public void recordSuccess(String username, String clientIp) {
        attempts.remove(userKey(username, clientIp));
        if (clientIp != null && !clientIp.isBlank()) {
            attempts.remove(ipKey(clientIp));
        }
    }

    /** Retrocompatível: registo de sucesso apenas por username. */
    public void recordSuccess(String username) {
        recordSuccess(username, null);
    }

    private static String userKey(String username, String clientIp) {
        return "user:" + (username == null ? "" : username.trim().toLowerCase(java.util.Locale.ROOT))
                + ":" + (clientIp == null || clientIp.isBlank() ? "local" : clientIp.trim());
    }

    private static String ipKey(String clientIp) {
        return "ip:" + (clientIp == null ? "" : clientIp.trim().toLowerCase());
    }

    private static final class Attempt {
        int count;
        Instant lockedUntil;
        Instant lastSeen = Instant.EPOCH;
    }

    private void prune(Instant now) {
        attempts.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().lastSeen.plus(lockout)));
        while (attempts.size() > maxTrackedKeys) {
            attempts.entrySet().stream()
                    .min(Comparator.comparing(entry -> entry.getValue().lastSeen))
                    .ifPresent(entry -> attempts.remove(entry.getKey(), entry.getValue()));
        }
    }

    int trackedKeys() {
        return attempts.size();
    }
}
