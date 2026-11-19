package mz.multicore.erp.architecture.security;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Where {@link AuthSessionService} keeps sessions. Sessions are addressed by the hash of their token, never the token. */
public interface AuthSessionStore {

    record StoredSession(String tokenHash, String username, Instant issuedAt, Instant expiresAt, boolean restricted) {}

    void save(StoredSession session);

    Optional<StoredSession> find(String tokenHash);

    void delete(String tokenHash);

    void deleteUser(String username);

    void deleteExpired(Instant now);

    List<String> hashesOldestFirst(String username);

    long count();
}
