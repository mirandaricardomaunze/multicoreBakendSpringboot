package mz.multicore.erp.modules.publicapi.dto;

import mz.multicore.erp.modules.publicapi.model.ApiKeyPermission;

import java.util.Set;

/** Who is calling the public API: the key, the company it belongs to and what it may do. */
public record ApiKeyPrincipal(Long keyId, Long companyId, String keyName, Set<ApiKeyPermission> permissions) {

    public ApiKeyPrincipal {
        permissions = Set.copyOf(permissions);
    }

    public boolean has(ApiKeyPermission permission) {
        return permissions.contains(permission);
    }
}
