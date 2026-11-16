package mz.multicore.erp.architecture.version;

import java.time.LocalDateTime;

/**
 * Company observed using an application version.
 *
 * @param companyName  name resolved on the <b>server</b>; the desktop must not cross-reference IDs
 *                     with another listing merely to display a name in a table
 * @param lastUsername last user observed on this version, identifying whom to contact
 */
public record ClientVersionUsageDTO(
        Long companyId,
        String companyName,
        String clientVersion,
        String lastUsername,
        LocalDateTime firstSeenAt,
        LocalDateTime lastSeenAt
) {}
