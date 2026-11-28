package mz.multicore.erp.architecture.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mz.multicore.erp.modules.users.service.AccountSecurityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PG-05 — um token de portal só é aceite em {@code /api/auth/**}, {@code /api/reports/store-dashboard},
 * {@code /api/approvals/**} e {@code /api/portal/**}; qualquer outra rota responde 403. Os tokens do desktop
 * continuam como hoje (PORTAL_GESTOR_SPEC §3.2).
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:portal-scope;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PortalTokenScopeTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AccountSecurityService accountSecurityService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void pg05PortalTokenOpensStoreDashboardAndApprovals() throws Exception {
        JsonNode login = portalLogin("ana");
        String token = login.get("token").asText();
        long companyId = login.get("companies").get(0).get("id").asLong();

        call(get("/api/reports/store-dashboard").param("companyId", String.valueOf(companyId)), token, companyId).andExpect(status().isOk());
        call(get("/api/approvals/pending"), token, companyId).andExpect(status().isOk());
    }

    @Test
    void pg05PortalTokenRefusedOutsidePortalRoutes() throws Exception {
        JsonNode login = portalLogin("ana");
        String token = login.get("token").asText();
        long companyId = login.get("companies").get(0).get("id").asLong();

        for (String path : new String[]{"/api/product-categories", "/api/users", "/api/inventory",
                "/api/approvalsx", "/api/portalx/summary", "/api/platform/companies", "/api/monitoring/health"}) {
            int status = call(get(path), token, companyId).andReturn().getResponse().getStatus();
            assertThat(status).as(path).isEqualTo(403);
        }
        call(post("/api/product-categories").contentType(MediaType.APPLICATION_JSON).content("{}"), token, companyId)
                .andExpect(status().isForbidden());
    }

    @Test
    void pg05PortalTokenKeepsAuthRoutes() throws Exception {
        String token = portalLogin("joao").get("token").asText();

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void pg05DesktopTokenUnchanged() throws Exception {
        PortalTwoFactorFixture.disable(jdbc, "ana");
        JsonNode login = json(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"password\":\"password\"}"))
                .andExpect(status().isOk()));
        String token = login.get("token").asText();
        long companyId = login.get("companies").get(0).get("id").asLong();

        call(get("/api/product-categories"), token, companyId).andExpect(status().isOk());
        call(get("/api/reports/store-dashboard").param("companyId", String.valueOf(companyId)), token, companyId).andExpect(status().isOk());
    }

    @Test
    void pg05ScopeMatchesWholePathSegments() {
        assertThat(PortalTokenScope.allows("/api/auth/logout")).isTrue();
        assertThat(PortalTokenScope.allows("/api/approvals")).isTrue();
        assertThat(PortalTokenScope.allows("/api/approvals/7/approve")).isTrue();
        assertThat(PortalTokenScope.allows("/api/portal/summary")).isTrue();
        assertThat(PortalTokenScope.allows("/api/reports/store-dashboard")).isTrue();
        assertThat(PortalTokenScope.allows("/api/reports/store-dashboard/export")).isFalse();
        assertThat(PortalTokenScope.allows("/api/portalx")).isFalse();
        assertThat(PortalTokenScope.allows("/api/products")).isFalse();
    }

    private ResultActions call(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                               String token, long companyId) throws Exception {
        return mockMvc.perform(request.header("Authorization", "Bearer " + token)
                .header("X-Company-Id", String.valueOf(companyId)));
    }

    private JsonNode portalLogin(String username) throws Exception {
        String secret = PortalTwoFactorFixture.enable(accountSecurityService, jdbc, username);
        String code = PortalTwoFactorFixture.currentCode(secret);
        return json(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"password","channel":"PORTAL","totpCode":"%s"}
                                """.formatted(username, code)))
                .andExpect(status().isOk()));
    }

    private JsonNode json(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
