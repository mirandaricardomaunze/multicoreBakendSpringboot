package mz.multicore.erp.architecture.security;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Process-local sessions for unit tests that build {@link AuthSessionService} without Spring. Not a bean. */
class InMemoryAuthSessionStore implements AuthSessionStore {

    private final Map<String, StoredSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(StoredSession session) {
        sessions.put(session.tokenHash(), session);
    }

    @Override
    public Optional<StoredSession> find(String tokenHash) {
        return Optional.ofNullable(sessions.get(tokenHash));
    }

    @Override
    public void delete(String tokenHash) {
        sessions.remove(tokenHash);
    }

    @Override
    public void updateExpiresAt(String tokenHash, Instant expiresAt) {
        sessions.computeIfPresent(tokenHash, (hash, s) -> new StoredSession(s.tokenHash(), s.username(),
                s.issuedAt(), expiresAt, s.restricted(), s.portal()));
    }

    @Override
    public void deleteUser(String username) {
        sessions.values().removeIf(session -> session.username().equals(username));
    }

    @Override
    public void deleteExpired(Instant now) {
        sessions.values().removeIf(session -> session.expiresAt().isBefore(now));
    }

    @Override
    public List<String> hashesOldestFirst(String username) {
        return sessions.values().stream().filter(session -> session.username().equals(username))
                .sorted(Comparator.comparing(StoredSession::issuedAt)).map(StoredSession::tokenHash).toList();
    }

    @Override
    public long count() {
        return sessions.size();
    }
}
