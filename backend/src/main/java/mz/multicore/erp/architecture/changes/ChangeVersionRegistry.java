package mz.multicore.erp.architecture.changes;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-company change counter that lets desktops refresh without polling every list.
 *
 * <p>Kept in memory: the {@link #epoch()} changes on every server start, so a client that sees a
 * new epoch treats it as a change instead of comparing counters from different server lives.
 */
@Component
public class ChangeVersionRegistry {

    private final String epoch = UUID.randomUUID().toString();
    private final Map<Long, AtomicLong> versions = new ConcurrentHashMap<>();

    public String epoch() {
        return epoch;
    }

    public long current(Long companyId) {
        if (companyId == null) return 0L;
        AtomicLong version = versions.get(companyId);
        return version == null ? 0L : version.get();
    }

    public long bump(Long companyId) {
        if (companyId == null) return 0L;
        return versions.computeIfAbsent(companyId, id -> new AtomicLong()).incrementAndGet();
    }

    public ChangeVersionDTO snapshot(Long companyId) {
        return new ChangeVersionDTO(epoch, current(companyId));
    }
}
