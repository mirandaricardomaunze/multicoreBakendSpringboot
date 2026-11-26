package mz.multicore.erp.modules.publicapi.service;

import mz.multicore.erp.modules.publicapi.dto.ApiKeyDTO;
import mz.multicore.erp.modules.publicapi.model.ApiKey;

import java.util.Set;

final class ApiKeyMapper {

    private ApiKeyMapper() {
    }

    static ApiKeyDTO toDTO(ApiKey key) {
        return new ApiKeyDTO(key.getId(), key.getName(), key.getKeyPrefix(), Set.copyOf(key.getPermissions()),
                key.getCreatedAt(), key.getCreatedBy(), key.getLastUsedAt(), key.getRevokedAt(),
                key.getRevokedBy(), key.isActive());
    }
}
