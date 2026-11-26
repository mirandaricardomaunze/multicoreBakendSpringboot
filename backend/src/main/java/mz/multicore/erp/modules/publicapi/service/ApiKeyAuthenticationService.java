package mz.multicore.erp.modules.publicapi.service;

import mz.multicore.erp.modules.publicapi.dto.ApiKeyPrincipal;
import mz.multicore.erp.modules.publicapi.model.ApiKey;
import mz.multicore.erp.modules.publicapi.repository.ApiKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Resolves the {@code X-Api-Key} of a public API request (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.1–§3.2): looks the key
 * up by its SHA-256 hash, refuses revoked keys at once and records the date of last use of every accepted key.
 */
@Service
public class ApiKeyAuthenticationService {

    private final ApiKeyRepository repository;

    public ApiKeyAuthenticationService(ApiKeyRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Optional<ApiKeyPrincipal> authenticate(String secret) {
        if (secret == null || secret.isBlank()) {
            return Optional.empty();
        }
        return repository.findByKeyHash(ApiKeySecrets.hash(secret.trim()))
                .filter(ApiKey::isActive)
                .map(this::markUsed);
    }

    private ApiKeyPrincipal markUsed(ApiKey key) {
        key.setLastUsedAt(LocalDateTime.now());
        repository.save(key);
        return new ApiKeyPrincipal(key.getId(), key.getCompany().getId(), key.getName(), key.getPermissions());
    }
}
