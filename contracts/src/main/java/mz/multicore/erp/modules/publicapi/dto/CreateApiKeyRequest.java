package mz.multicore.erp.modules.publicapi.dto;

import mz.multicore.erp.modules.publicapi.model.ApiKeyPermission;

import java.util.Set;

/** ADMIN request to create a public API key with a name (e.g. "Loja online") and its permissions. */
public record CreateApiKeyRequest(String name, Set<ApiKeyPermission> permissions) {
}
