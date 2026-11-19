package mz.multicore.erp.architecture.security;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.users.model.AppUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Issues, validates and revokes the opaque bearer tokens of logged-in users. Sessions last 8 hours, a user keeps at
 * most {@link #MAX_SESSIONS_PER_USER} (the oldest falls first) and only the SHA-256 of each token is stored, in the
 * database, so a restart or a second instance keeps everyone signed in (SC-09, SC-10, SC-33..SC-35).
 */
@Service
public class AuthSessionService {

    private static final Duration SESSION_LIFETIME = Duration.ofHours(8);
    static final int MAX_SESSIONS_PER_USER = 10;
    private final AuthSessionStore store;
    private Instant lastIssuedAt = Instant.EPOCH;

    @Autowired
    public AuthSessionService(AuthSessionStore store) {
        this.store = store;
    }

    /** Process-local sessions, for unit tests built without Spring. */
    public AuthSessionService() {
        this(new InMemoryAuthSessionStore());
    }

    public synchronized AuthSession create(AppUser user) {
        return create(user, false);
    }

    /** @param restricted the session only serves to finish securing the account (password change, 2FA setup) */
    public synchronized AuthSession create(AppUser user, boolean restricted) {
        Instant now = nextIssuedAt();
        store.deleteExpired(now);
        revokeOldestBeyondLimit(user.getUsername());
        AuthSession session = new AuthSession(UUID.randomUUID().toString(), user.getUsername(), now,
                now.plus(SESSION_LIFETIME), restricted);
        store.save(new AuthSessionStore.StoredSession(hash(session.token()), session.username(),
                session.issuedAt(), session.expiresAt(), restricted));
        return session;
    }

    public AuthSession requireValid(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessRuleException("Token de autenticação em falta.");
        }
        String tokenHash = hash(token);
        AuthSessionStore.StoredSession stored = store.find(tokenHash).orElse(null);
        if (stored == null || stored.expiresAt().isBefore(Instant.now())) {
            if (stored != null) store.delete(tokenHash);
            throw new BusinessRuleException("Sessão inválida ou expirada.");
        }
        return new AuthSession(token, stored.username(), stored.issuedAt(), stored.expiresAt(), stored.restricted());
    }

    public void revoke(String token) {
        if (token != null && !token.isBlank()) {
            store.delete(hash(token));
        }
    }

    public void revokeUser(String username) {
        if (username != null) {
            store.deleteUser(username);
        }
    }

    /** Without this, every login would leave a row behind until its token was used again. */
    @Scheduled(fixedDelay = 600_000)
    public void purgeExpired() {
        purgeExpired(Instant.now());
    }

    void purgeExpired(Instant now) {
        store.deleteExpired(now);
    }

    int activeSessions() {
        return (int) store.count();
    }

    private void revokeOldestBeyondLimit(String username) {
        List<String> owned = store.hashesOldestFirst(username);
        owned.stream().limit(Math.max(0, owned.size() - MAX_SESSIONS_PER_USER + 1)).forEach(store::delete);
    }

    /** No ties, at the microsecond precision the database keeps: the per-user limit must know the oldest session. */
    private Instant nextIssuedAt() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        lastIssuedAt = now.isAfter(lastIssuedAt) ? now : lastIssuedAt.plus(1, ChronoUnit.MICROS);
        return lastIssuedAt;
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM.", e);
        }
    }

    public record AuthSession(String token, String username, Instant issuedAt, Instant expiresAt, boolean restricted) {
        public AuthSession(String token, String username, Instant issuedAt, Instant expiresAt) {
            this(token, username, issuedAt, expiresAt, false);
        }
    }
}
