package mz.multicore.erp.modules.publicapi.dto;

import mz.multicore.erp.modules.publicapi.model.ApiKeyPermission;

import java.time.LocalDateTime;
import java.util.Set;

/** A public API key as listed to the ADMIN: never carries the secret nor its hash. */
public record ApiKeyDTO(Long id, String name, String keyPrefix, Set<ApiKeyPermission> permissions,
                        LocalDateTime createdAt, String createdBy, LocalDateTime lastUsedAt,
                        LocalDateTime revokedAt, String revokedBy, boolean active) {
}
