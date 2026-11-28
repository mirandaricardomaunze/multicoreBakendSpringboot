package mz.multicore.erp.modules.publicapi.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.publicapi.dto.ApiKeyDTO;
import mz.multicore.erp.modules.publicapi.dto.CreateApiKeyRequest;
import mz.multicore.erp.modules.publicapi.dto.CreatedApiKeyDTO;
import mz.multicore.erp.modules.publicapi.model.ApiKey;
import mz.multicore.erp.modules.publicapi.repository.ApiKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * Public API keys of the active company (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.1, API-01): the ADMIN creates a key with
 * a name and permissions, sees the full secret only in the creation result, lists keys without the secret and revokes
 * them with immediate effect. Creating and revoking are audited as {@code API_KEY_CREATE} / {@code API_KEY_REVOKE}.
 */
@Service
public class ApiKeyService {

    private static final int MAX_NAME_LENGTH = 100;

    private final ApiKeyRepository repository;
    private final CompanyService companyService;
    private final AuditLogService auditLogService;

    public ApiKeyService(ApiKeyRepository repository, CompanyService companyService,
                         AuditLogService auditLogService) {
        this.repository = repository;
        this.companyService = companyService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<ApiKeyDTO> list() {
        PermissionGuard.requireAdmin("consultar as chaves de API");
        return repository.findByCompanyIdOrderByCreatedAtDescIdDesc(CurrentUserContext.requireCurrentCompanyId())
                .stream().map(ApiKeyMapper::toDTO).toList();
    }

    @Transactional
    public CreatedApiKeyDTO create(CreateApiKeyRequest request) {
        PermissionGuard.requireAdmin("criar chaves de API");
        String name = requireName(request.name());
        if (request.permissions() == null || request.permissions().isEmpty()) {
            throw new BusinessRuleException("Escolha pelo menos uma permissão para a chave de API.");
        }
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        String secret = ApiKeySecrets.generate();

        ApiKey key = new ApiKey();
        key.setCompany(companyService.getCurrentCompanyReference(companyId));
        key.setName(name);
        key.setKeyPrefix(ApiKeySecrets.identifier(secret));
        key.setKeyHash(ApiKeySecrets.hash(secret));
        key.setPermissions(EnumSet.copyOf(request.permissions()));
        key.setCreatedBy(CurrentUserContext.getUsername());
        ApiKey saved = repository.save(key);

        auditLogService.logCurrent("API_KEY_CREATE", describe(saved) + " com permissões " + saved.getPermissions());
        return new CreatedApiKeyDTO(ApiKeyMapper.toDTO(saved), secret);
    }

    @Transactional
    public ApiKeyDTO revoke(Long id) {
        PermissionGuard.requireAdmin("revogar chaves de API");
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        ApiKey key = repository.findById(id)
                .filter(found -> Objects.equals(found.getCompany().getId(), companyId))
                .orElseThrow(() -> new BusinessRuleException("Chave de API não encontrada nesta empresa."));
        if (!key.isActive()) {
            throw new BusinessRuleException("Esta chave de API já está revogada.");
        }
        key.setRevokedAt(LocalDateTime.now());
        key.setRevokedBy(CurrentUserContext.getUsername());
        ApiKey saved = repository.save(key);

        auditLogService.logCurrent("API_KEY_REVOKE", describe(saved));
        return ApiKeyMapper.toDTO(saved);
    }

    private static String requireName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) {
            throw new BusinessRuleException("Indique o nome da chave de API (ex.: Loja online).");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new BusinessRuleException("O nome da chave de API tem no máximo " + MAX_NAME_LENGTH + " caracteres.");
        }
        return name;
    }

    private static String describe(ApiKey key) {
        return "Chave de API \"" + key.getName() + "\" (" + ApiKeySecrets.PREFIX + key.getKeyPrefix() + "...)";
    }
}
