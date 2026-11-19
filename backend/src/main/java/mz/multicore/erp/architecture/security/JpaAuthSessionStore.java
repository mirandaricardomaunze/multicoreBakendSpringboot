package mz.multicore.erp.architecture.security;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Database-backed sessions: a backend restart keeps everyone signed in and every instance sees the same sessions. */
@Component
public class JpaAuthSessionStore implements AuthSessionStore {

    private final AuthSessionRecordRepository repository;

    public JpaAuthSessionStore(AuthSessionRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void save(StoredSession session) {
        AuthSessionRecord row = new AuthSessionRecord();
        row.setTokenHash(session.tokenHash());
        row.setUsername(session.username());
        row.setIssuedAt(session.issuedAt());
        row.setExpiresAt(session.expiresAt());
        row.setRestricted(session.restricted());
        row.setCreatedBy(session.username());
        repository.save(row);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredSession> find(String tokenHash) {
        return repository.findById(tokenHash).map(row -> new StoredSession(row.getTokenHash(), row.getUsername(),
                row.getIssuedAt(), row.getExpiresAt(), row.isRestricted()));
    }

    @Override
    @Transactional
    public void delete(String tokenHash) {
        repository.deleteById(tokenHash);
    }

    @Override
    @Transactional
    public void deleteUser(String username) {
        repository.deleteByUsername(username);
    }

    @Override
    @Transactional
    public void deleteExpired(Instant now) {
        repository.deleteExpired(now);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> hashesOldestFirst(String username) {
        return repository.findHashesOldestFirst(username);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return repository.count();
    }
}
