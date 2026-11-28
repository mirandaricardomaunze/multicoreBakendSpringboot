package mz.multicore.erp.modules.publicapi.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.publicapi.dto.ApiKeyDTO;
import mz.multicore.erp.modules.publicapi.dto.ApiKeyPrincipal;
import mz.multicore.erp.modules.publicapi.dto.CreateApiKeyRequest;
import mz.multicore.erp.modules.publicapi.dto.CreatedApiKeyDTO;
import mz.multicore.erp.modules.publicapi.model.ApiKey;
import mz.multicore.erp.modules.publicapi.model.ApiKeyPermission;
import mz.multicore.erp.modules.publicapi.repository.ApiKeyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Harness API-01 (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.1): the key is {@code mk_live_} + 32 random bytes in base62,
 * shown only once; the server keeps only its SHA-256 hash and the first 8 characters for identification. Revoking
 * takes effect on the next request and every accepted request records the date of last use.
 */
class ApiKeyRulesTest {

    private static final Long COMPANY_ID = 1L;
    private static final Long OTHER_COMPANY_ID = 2L;

    private final ApiKeyRepository repository = mock(ApiKeyRepository.class);
    private final CompanyService companyService = mock(CompanyService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final ApiKeyService service = new ApiKeyService(repository, companyService, auditLogService);
    private final ApiKeyAuthenticationService authentication = new ApiKeyAuthenticationService(repository);

    private final Map<String, ApiKey> byHash = new HashMap<>();
    private final Map<Long, ApiKey> byId = new HashMap<>();

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        when(companyService.getCurrentCompanyReference(COMPANY_ID)).thenReturn(company(COMPANY_ID));
        when(repository.save(any(ApiKey.class))).thenAnswer(invocation -> {
            ApiKey key = invocation.getArgument(0);
            if (key.getId() == null) key.setId(100L + byId.size());
            byId.put(key.getId(), key);
            byHash.put(key.getKeyHash(), key);
            return key;
        });
        when(repository.findByKeyHash(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(byHash.get(invocation.<String>getArgument(0))));
        when(repository.findById(any()))
                .thenAnswer(invocation -> Optional.ofNullable(byId.get(invocation.<Long>getArgument(0))));
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void api01_chaveTemPrefixoE32BytesEmBase62() {
        String secret = create("Loja online", ApiKeyPermission.CATALOG_READ).secret();

        assertThat(secret).startsWith("mk_live_");
        String random = secret.substring("mk_live_".length());
        assertThat(random).hasSize(43).matches("[0-9A-Za-z]+");
        assertNotEquals(secret, create("Contabilista", ApiKeyPermission.ORDERS_READ).secret());
    }

    @Test
    void api01_servidorGuardaSoOHashEOsPrimeiros8Caracteres() throws Exception {
        CreatedApiKeyDTO created = create("Loja online", ApiKeyPermission.CATALOG_READ, ApiKeyPermission.ORDERS_WRITE);

        ArgumentCaptor<ApiKey> saved = ArgumentCaptor.forClass(ApiKey.class);
        verify(repository).save(saved.capture());
        ApiKey key = saved.getValue();
        assertEquals(sha256(created.secret()), key.getKeyHash());
        assertEquals(created.secret().substring("mk_live_".length(), "mk_live_".length() + 8), key.getKeyPrefix());
        assertThat(stringFieldsOf(key)).doesNotContain(created.secret());
        assertEquals(COMPANY_ID, key.getCompany().getId());
        assertEquals(EnumSet.of(ApiKeyPermission.CATALOG_READ, ApiKeyPermission.ORDERS_WRITE), key.getPermissions());
        assertEquals("Loja online", created.key().name());
        assertEquals(key.getKeyPrefix(), created.key().keyPrefix());
    }

    @Test
    void api01_chaveSoEMostradaUmaVez_listagemNaoTemSegredo() {
        CreatedApiKeyDTO created = create("Loja online", ApiKeyPermission.CATALOG_READ);
        when(repository.findByCompanyIdOrderByCreatedAtDescIdDesc(COMPANY_ID)).thenReturn(List.copyOf(byId.values()));

        List<ApiKeyDTO> listed = service.list();

        assertThat(listed).hasSize(1);
        assertThat(listed.get(0).toString()).doesNotContain(created.secret());
        assertThat(Arrays.stream(ApiKeyDTO.class.getRecordComponents()).map(c -> c.getName()))
                .doesNotContain("secret", "keyHash");
    }

    @Test
    void api01_nomeEPermissoesSaoObrigatorios() {
        assertThrows(BusinessRuleException.class,
                () -> service.create(new CreateApiKeyRequest("  ", Set.of(ApiKeyPermission.CATALOG_READ))));
        assertThrows(BusinessRuleException.class, () -> service.create(new CreateApiKeyRequest("Loja", Set.of())));
        assertThrows(BusinessRuleException.class, () -> service.create(new CreateApiKeyRequest("Loja", null)));
        verify(repository, never()).save(any());
    }

    @Test
    void api01_chaveValidaAutenticaComAEmpresaEPermissoesERegistaUltimoUso() {
        CreatedApiKeyDTO created = create("Loja online", ApiKeyPermission.CATALOG_READ);
        CurrentUserContext.clear();
        LocalDateTime before = LocalDateTime.now();

        ApiKeyPrincipal principal = authentication.authenticate(created.secret()).orElseThrow();

        assertEquals(created.key().id(), principal.keyId());
        assertEquals(COMPANY_ID, principal.companyId());
        assertEquals("Loja online", principal.keyName());
        assertEquals(Set.of(ApiKeyPermission.CATALOG_READ), principal.permissions());
        assertThat(byId.get(created.key().id()).getLastUsedAt()).isBetween(before, LocalDateTime.now());
    }

    @Test
    void api01_chaveDesconhecidaOuVaziaNaoAutentica() {
        create("Loja online", ApiKeyPermission.CATALOG_READ);

        assertThat(authentication.authenticate("mk_live_inexistente")).isEmpty();
        assertThat(authentication.authenticate("")).isEmpty();
        assertThat(authentication.authenticate(null)).isEmpty();
    }

    @Test
    void api01_revogarEImediatoEAuditado() {
        CreatedApiKeyDTO created = create("Loja online", ApiKeyPermission.CATALOG_READ);
        assertThat(authentication.authenticate(created.secret())).isPresent();

        ApiKeyDTO revoked = service.revoke(created.key().id());

        assertThat(revoked.revokedAt()).isNotNull();
        assertThat(revoked.active()).isFalse();
        assertThat(authentication.authenticate(created.secret())).isEmpty();
        verify(auditLogService).logCurrent(eq("API_KEY_REVOKE"), contains("Loja online"));
        assertThrows(BusinessRuleException.class, () -> service.revoke(created.key().id()));
    }

    @Test
    void api01_criarEAuditadoSemOSegredo() {
        CreatedApiKeyDTO created = create("Loja online", ApiKeyPermission.CATALOG_READ);

        ArgumentCaptor<String> details = ArgumentCaptor.forClass(String.class);
        verify(auditLogService).logCurrent(eq("API_KEY_CREATE"), details.capture());
        assertThat(details.getValue()).contains("Loja online").doesNotContain(created.secret());
    }

    @Test
    void api01_naoRevogaChaveDeOutraEmpresa() {
        CreatedApiKeyDTO created = create("Loja online", ApiKeyPermission.CATALOG_READ);
        byId.get(created.key().id()).setCompany(company(OTHER_COMPANY_ID));

        assertThrows(BusinessRuleException.class, () -> service.revoke(created.key().id()));
        assertThat(byId.get(created.key().id()).getRevokedAt()).isNull();
    }

    private CreatedApiKeyDTO create(String name, ApiKeyPermission... permissions) {
        return service.create(new CreateApiKeyRequest(name, Set.of(permissions)));
    }

    private static Company company(Long id) {
        Company company = new Company();
        company.setId(id);
        return company;
    }

    private static String sha256(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        return java.util.HexFormat.of().formatHex(digest);
    }

    private static List<String> stringFieldsOf(ApiKey key) throws IllegalAccessException {
        List<String> values = new java.util.ArrayList<>();
        for (Field field : ApiKey.class.getDeclaredFields()) {
            if (field.getType() == String.class) {
                field.setAccessible(true);
                values.add((String) field.get(key));
            }
        }
        return values;
    }
}
