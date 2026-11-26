package mz.multicore.erp.architecture.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mz.multicore.erp.modules.publicapi.dto.ApiKeyPrincipal;
import mz.multicore.erp.modules.publicapi.dto.CreateApiKeyRequest;
import mz.multicore.erp.modules.publicapi.model.ApiKeyPermission;
import mz.multicore.erp.modules.publicapi.service.ApiKeyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Harness API-03 and API-04 (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.2, §3.3): {@code /api/public/v1/**} only opens with the
 * {@code X-Api-Key} header and the company is the key's (no {@code X-Company-Id}); user tokens are refused there
 * and API keys are refused on the internal {@code /api/**}. Errors are JSON with a stable {@code code} and a PT-MZ
 * {@code message}. Each route demands its own key permission; a valid key without it → 403
 * {@code API_PERMISSION_DENIED}.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:public-api-auth;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PublicApiAuthenticationTest.ProbeController.class)
class PublicApiAuthenticationTest {

    private static final String PROBE = "/api/public/v1/__probe";

    @Autowired private MockMvc mockMvc;
    @Autowired private ApiKeyService apiKeyService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;

    /** Stand-in for the public resources (API-07..10): echoes who the key says is calling. */
    @RestController
    static class ProbeController {
        @GetMapping(PROBE)
        Map<String, Object> probe(@AuthenticationPrincipal ApiKeyPrincipal principal) {
            return Map.of("companyId", principal.companyId(), "keyName", principal.keyName());
        }
    }

    @AfterEach
    void cleanUp() {
        CurrentUserContext.clear();
        jdbc.update("delete from api_keys where name like 'API-03%' or name like 'API-04%'");
    }

    @Test
    void api03ValidKeyOpensPublicApiWithItsCompanyAndNoCompanyHeader() throws Exception {
        long companyId = anyCompany();
        String secret = createKey(companyId, "API-03 Loja online");

        JsonNode body = json(mockMvc.perform(get(PROBE).header("X-Api-Key", secret))
                .andExpect(status().isOk()).andReturn().getResponse());

        assertThat(body.get("companyId").asLong()).isEqualTo(companyId);
        assertThat(body.get("keyName").asText()).isEqualTo("API-03 Loja online");
        assertThat(jdbc.queryForObject("select last_used_at from api_keys where name = 'API-03 Loja online'",
                Object.class)).isNotNull();
    }

    @Test
    void api03MissingKeyIsRefusedWithJsonError() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get(PROBE))
                .andExpect(status().isUnauthorized()).andReturn().getResponse();

        assertError(response, "API_KEY_REQUIRED");
    }

    @Test
    void api03UnknownOrRevokedKeyIsRefused() throws Exception {
        MockHttpServletResponse unknown = mockMvc.perform(get(PROBE)
                        .header("X-Api-Key", "mk_live_0000000000000000000000000000000000000000000"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse();
        assertError(unknown, "API_KEY_INVALID");

        long companyId = anyCompany();
        String secret = createKey(companyId, "API-03 Revogada");
        Long id = jdbc.queryForObject("select id from api_keys where name = 'API-03 Revogada'", Long.class);
        asAdmin(companyId);
        apiKeyService.revoke(id);
        CurrentUserContext.clear();

        MockHttpServletResponse revoked = mockMvc.perform(get(PROBE).header("X-Api-Key", secret))
                .andExpect(status().isUnauthorized()).andReturn().getResponse();
        assertError(revoked, "API_KEY_INVALID");
    }

    @Test
    void api03UserTokenIsRefusedOnPublicApi() throws Exception {
        String token = desktopLogin();

        MockHttpServletResponse response = mockMvc.perform(get(PROBE)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Company-Id", String.valueOf(anyCompany())))
                .andExpect(status().isUnauthorized()).andReturn().getResponse();

        assertError(response, "USER_TOKEN_NOT_ACCEPTED");
    }

    @Test
    void api03ApiKeyIsRefusedOnInternalApi() throws Exception {
        long companyId = anyCompany();
        String secret = createKey(companyId, "API-03 Interna");

        mockMvc.perform(get("/api/product-categories")
                        .header("X-Api-Key", secret)
                        .header("X-Company-Id", String.valueOf(companyId)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void api03PublicPrefixIsMatchedBySegment() {
        assertThat(PublicApiRoutes.isPublicApi("/api/public/v1/products")).isTrue();
        assertThat(PublicApiRoutes.isPublicApi("/api/public/v1")).isTrue();
        assertThat(PublicApiRoutes.isPublicApi("/api/publicx/v1/products")).isFalse();
        assertThat(PublicApiRoutes.isPublicApi("/api/products")).isFalse();
    }

    @Test
    void api04EachRouteDemandsItsPermission() {
        assertThat(PublicApiPermissions.required("GET", "/api/public/v1/products"))
                .contains(ApiKeyPermission.CATALOG_READ);
        assertThat(PublicApiPermissions.required("GET", "/api/public/v1/products/SKU-1"))
                .contains(ApiKeyPermission.CATALOG_READ);
        assertThat(PublicApiPermissions.required("GET", "/api/public/v1/stock"))
                .contains(ApiKeyPermission.CATALOG_READ);
        assertThat(PublicApiPermissions.required("GET", "/api/public/v1/orders/ENC-1"))
                .contains(ApiKeyPermission.ORDERS_READ);
        assertThat(PublicApiPermissions.required("POST", "/api/public/v1/orders"))
                .contains(ApiKeyPermission.ORDERS_WRITE);
        assertThat(PublicApiPermissions.required("GET", "/api/public/v1/productsx")).isEmpty();
    }

    @Test
    void api04KeyWithoutRoutePermissionIsForbiddenWithJsonError() throws Exception {
        long companyId = anyCompany();
        String catalogOnly = createKey(companyId, "API-04 Catalogo", EnumSet.of(ApiKeyPermission.CATALOG_READ));

        assertError(mockMvc.perform(get("/api/public/v1/orders/ENC-1").header("X-Api-Key", catalogOnly))
                .andExpect(status().isForbidden()).andReturn().getResponse(), "API_PERMISSION_DENIED");
        assertError(mockMvc.perform(post("/api/public/v1/orders").header("X-Api-Key", catalogOnly)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden()).andReturn().getResponse(), "API_PERMISSION_DENIED");

        String ordersWrite = createKey(companyId, "API-04 Escrita", EnumSet.of(ApiKeyPermission.ORDERS_WRITE));
        assertError(mockMvc.perform(get("/api/public/v1/orders/ENC-1").header("X-Api-Key", ordersWrite))
                .andExpect(status().isForbidden()).andReturn().getResponse(), "API_PERMISSION_DENIED");
        assertError(mockMvc.perform(get("/api/public/v1/products").header("X-Api-Key", ordersWrite))
                .andExpect(status().isForbidden()).andReturn().getResponse(), "API_PERMISSION_DENIED");
    }

    @Test
    void api04KeyWithRoutePermissionPassesTheGate() throws Exception {
        long companyId = anyCompany();
        String catalog = createKey(companyId, "API-04 Leitura", EnumSet.of(ApiKeyPermission.CATALOG_READ));

        int status = mockMvc.perform(get("/api/public/v1/products").header("X-Api-Key", catalog))
                .andReturn().getResponse().getStatus();

        assertThat(status).isNotIn(401, 403);
    }

    private void assertError(MockHttpServletResponse response, String code) throws Exception {
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        JsonNode error = json(response);
        assertThat(error.get("code").asText()).isEqualTo(code);
        assertThat(error.get("message").asText()).isNotBlank();
        assertThat(response.getContentAsString()).doesNotContain("Exception", "at mz.");
    }

    private String createKey(long companyId, String name) {
        return createKey(companyId, name, EnumSet.of(ApiKeyPermission.CATALOG_READ));
    }

    private String createKey(long companyId, String name, Set<ApiKeyPermission> permissions) {
        asAdmin(companyId);
        try {
            return apiKeyService.create(new CreateApiKeyRequest(name, permissions)).secret();
        } finally {
            CurrentUserContext.clear();
        }
    }

    private static void asAdmin(long companyId) {
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(companyId);
    }

    private long anyCompany() {
        return jdbc.queryForObject("select min(id) from companies", Long.class);
    }

    private JsonNode json(MockHttpServletResponse response) throws Exception {
        return objectMapper.readTree(response.getContentAsString());
    }

    private String desktopLogin() throws Exception {
        PortalTwoFactorFixture.disable(jdbc, "joao");
        return json(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"joao\",\"password\":\"password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse()).get("token").asText();
    }
}
