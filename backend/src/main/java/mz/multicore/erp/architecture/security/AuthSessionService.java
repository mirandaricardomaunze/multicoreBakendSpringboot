package mz.multicore.erp.architecture.security;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.users.model.AppUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
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
 * Portal sessions (PORTAL_GESTOR_SPEC §3.1, PG-03) end 12 hours after login or 30 minutes after the last request,
 * whichever comes first: their expiry slides forward on use but never past the 12-hour cap.
 */
@Service
public class AuthSessionService {

    private static final Duration SESSION_LIFETIME = Duration.ofHours(8);
    static final Duration PORTAL_MAX_LIFETIME = Duration.ofHours(12);
    static final Duration PORTAL_IDLE_TIMEOUT = Duration.ofMinutes(30);
    /** A portal request only rewrites the expiry when it moves by at least this much (one write a minute, not per request). */
    private static final Duration PORTAL_SLIDE_STEP = Duration.ofMinutes(1);
    static final int MAX_SESSIONS_PER_USER = 10;
    private final AuthSessionStore store;
    private final Clock clock;
    private Instant lastIssuedAt = Instant.EPOCH;

    @Autowired
    public AuthSessionService(AuthSessionStore store) {
        this(store, Clock.systemUTC());
    }

    AuthSessionService(AuthSessionStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
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
        return create(user, restricted, false);
    }

    /** @param portal session of the manager web portal: 12 h cap and 30 min idle timeout (PG-03) */
    public synchronized AuthSession create(AppUser user, boolean restricted, boolean portal) {
        Instant now = nextIssuedAt();
        store.deleteExpired(now);
        revokeOldestBeyondLimit(user.getUsername());
        Instant expiresAt = portal ? now.plus(PORTAL_IDLE_TIMEOUT) : now.plus(SESSION_LIFETIME);
        AuthSession session = new AuthSession(UUID.randomUUID().toString(), user.getUsername(), now,
                expiresAt, restricted, portal);
        store.save(new AuthSessionStore.StoredSession(hash(session.token()), session.username(),
                session.issuedAt(), session.expiresAt(), restricted, portal));
        return session;
    }

    public AuthSession requireValid(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessRuleException("Token de autenticação em falta.");
        }
        String tokenHash = hash(token);
        AuthSessionStore.StoredSession stored = store.find(tokenHash).orElse(null);
        Instant now = clock.instant();
        if (stored == null || !stored.expiresAt().isAfter(now)) {
            if (stored != null) store.delete(tokenHash);
            throw new BusinessRuleException("Sessão inválida ou expirada.");
        }
        Instant expiresAt = stored.portal() ? slidePortalExpiry(stored, now) : stored.expiresAt();
        return new AuthSession(token, stored.username(), stored.issuedAt(), expiresAt, stored.restricted(),
                stored.portal());
    }

    /** Each use pushes the idle deadline to now + 30 min, capped at 12 h after login. */
    private Instant slidePortalExpiry(AuthSessionStore.StoredSession stored, Instant now) {
        Instant cap = stored.issuedAt().plus(PORTAL_MAX_LIFETIME);
        Instant idle = now.plus(PORTAL_IDLE_TIMEOUT);
        Instant next = idle.isBefore(cap) ? idle : cap;
        if (Duration.between(stored.expiresAt(), next).compareTo(PORTAL_SLIDE_STEP) < 0) {
            return stored.expiresAt();
        }
        store.updateExpiresAt(stored.tokenHash(), next);
        return next;
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
        purgeExpired(clock.instant());
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
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
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

    public record AuthSession(String token, String username, Instant issuedAt, Instant expiresAt, boolean restricted,
                              boolean portal) {
        public AuthSession(String token, String username, Instant issuedAt, Instant expiresAt) {
            this(token, username, issuedAt, expiresAt, false, false);
        }

        public AuthSession(String token, String username, Instant issuedAt, Instant expiresAt, boolean restricted) {
            this(token, username, issuedAt, expiresAt, restricted, false);
        }
    }
}
