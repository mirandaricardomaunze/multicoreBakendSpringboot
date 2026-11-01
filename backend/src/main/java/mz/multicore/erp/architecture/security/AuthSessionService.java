package mz.multicore.erp.architecture.security;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.users.model.AppUser;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthSessionService {

    private static final Duration SESSION_LIFETIME = Duration.ofHours(8);
    static final int MAX_SESSIONS_PER_USER = 10;
    private final Map<String, AuthSession> sessions = new ConcurrentHashMap<>();

    public synchronized AuthSession create(AppUser user) {
        return create(user, false);
    }

    /** @param restricted a sessão só serve para concluir a segurança da conta */
    public synchronized AuthSession create(AppUser user, boolean restricted) {
        Instant now = Instant.now();
        purgeExpired(now);
        revokeOldestBeyondLimit(user.getUsername());
        AuthSession session = new AuthSession(
                UUID.randomUUID().toString(),
                user.getUsername(),
                now,
                now.plus(SESSION_LIFETIME),
                restricted
        );
        sessions.put(session.token(), session);
        return session;
    }

    public AuthSession requireValid(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessRuleException("Token de autenticação em falta.");
        }
        AuthSession session = sessions.get(token);
        if (session == null || session.expiresAt().isBefore(Instant.now())) {
            sessions.remove(token);
            throw new BusinessRuleException("Sessão inválida ou expirada.");
        }
        return session;
    }

    public void revoke(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    public void revokeUser(String username) {
        if (username != null) {
            sessions.entrySet().removeIf(entry -> username.equals(entry.getValue().username()));
        }
    }

    /** Sem isto, cada login deixava uma entrada em memória até o seu token voltar a ser usado. */
    @Scheduled(fixedDelay = 600_000)
    public void purgeExpired() {
        purgeExpired(Instant.now());
    }

    void purgeExpired(Instant now) {
        sessions.values().removeIf(session -> session.expiresAt().isBefore(now));
    }

    int activeSessions() {
        return sessions.size();
    }

    private void revokeOldestBeyondLimit(String username) {
        List<AuthSession> owned = sessions.values().stream()
                .filter(session -> session.username().equals(username))
                .sorted(Comparator.comparing(AuthSession::issuedAt))
                .toList();
        owned.stream().limit(Math.max(0, owned.size() - MAX_SESSIONS_PER_USER + 1))
                .forEach(session -> sessions.remove(session.token()));
    }

    public record AuthSession(String token, String username, Instant issuedAt, Instant expiresAt, boolean restricted) {
        public AuthSession(String token, String username, Instant issuedAt, Instant expiresAt) {
            this(token, username, issuedAt, expiresAt, false);
        }
    }
}
