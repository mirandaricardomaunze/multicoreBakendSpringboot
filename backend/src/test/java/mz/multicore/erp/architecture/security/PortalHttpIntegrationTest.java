package mz.multicore.erp.architecture.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mz.multicore.erp.modules.users.service.AccountSecurityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PG-06 — isolamento de empresa: o portal só abre as empresas do utilizador onde ele é ADMIN ou MANAGER;
 * os pedidos levam {@code X-Company-Id} e respeitam o isolamento existente (PORTAL_GESTOR_SPEC §3.2).
 * PG-07 — {@code /api/portal/summary} devolve os blocos do painel, só para ADMIN/MANAGER da empresa (§3.3).
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:portal-http;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PortalHttpIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AccountSecurityService accountSecurityService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;

    @AfterEach
    void removeExtraAccess() {
        jdbc.update("delete from app_user_companies where user_id = (select id from app_users where username = 'joao')"
                + " and company_id <> ?", companyId("Multicore Portugal Lda"));
    }

    @Test
    void pg06PortalOpensOwnCompany() throws Exception {
        JsonNode login = portalLogin("joao");
        long companyId = login.get("companies").get(0).get("id").asLong();

        call(get("/api/approvals/pending"), login.get("token").asText(), companyId).andExpect(status().isOk());
    }

    @Test
    void pg06PortalRefusesCompanyWithoutAccess() throws Exception {
        String token = portalLogin("joao").get("token").asText();
        long otherCompany = companyId("Multicore Moçambique Lda");

        call(get("/api/approvals/pending"), token, otherCompany).andExpect(status().isForbidden());
        call(get("/api/reports/store-dashboard").param("companyId", String.valueOf(otherCompany)), token, otherCompany)
                .andExpect(status().isForbidden());
    }

    @Test
    void pg06PortalRefusesCompanyWhereUserIsOnlyEmployee() throws Exception {
        long mz = companyId("Multicore Moçambique Lda");
        grantJoao(mz, "EMPLOYEE");
        JsonNode login = portalLogin("joao");
        assertThat(login.get("companies")).hasSize(1);
        String token = login.get("token").asText();

        ResultActions refused = call(get("/api/approvals/pending"), token, mz).andExpect(status().isForbidden());
        assertThat(refused.andReturn().getResponse().getErrorMessage())
                .isEqualTo(PortalLoginPolicy.COMPANY_REFUSED_MESSAGE);
    }

    @Test
    void pg06PortalRefusesUrlCompanyDifferentFromHeader() throws Exception {
        long mz = companyId("Multicore Moçambique Lda");
        JsonNode login = portalLogin("joao");
        long own = login.get("companies").get(0).get("id").asLong();

        call(get("/api/reports/store-dashboard").param("companyId", String.valueOf(mz)), login.get("token").asText(), own)
                .andExpect(status().isForbidden());
    }

    @Test
    void pg06DesktopTokenKeepsSellerCompany() throws Exception {
        long mz = companyId("Multicore Moçambique Lda");
        grantJoao(mz, "SELLER");
        String token = desktopLogin("joao");

        call(get("/api/product-categories"), token, mz).andExpect(status().isOk());
    }

    @Test
    void pg06PortalRefusesCompanyWhereUserIsOnlySeller() throws Exception {
        long mz = companyId("Multicore Moçambique Lda");
        grantJoao(mz, "SELLER");
        String token = portalLogin("joao").get("token").asText();

        call(get("/api/reports/store-dashboard").param("companyId", String.valueOf(mz)), token, mz)
                .andExpect(status().isForbidden());
    }

    @Test
    void pg07PortalSummaryReturnsDashboardBlocks() throws Exception {
        JsonNode login = portalLogin("joao");
        long companyId = login.get("companies").get(0).get("id").asLong();

        JsonNode summary = objectMapper.readTree(call(get("/api/portal/summary")
                        .param("companyId", String.valueOf(companyId)), login.get("token").asText(), companyId)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        assertThat(summary.fieldNames()).toIterable().contains("date", "salesToday", "salesMonth", "salesByStore",
                "openTills", "lowStock", "overdueTotal", "overdueClients", "pendingApprovals");
        assertThat(summary.get("lowStock").size()).isLessThanOrEqualTo(20);
        assertThat(summary.get("overdueClients").size()).isLessThanOrEqualTo(10);
    }

    @Test
    void pg07PortalSummaryRefusesOtherCompany() throws Exception {
        String token = portalLogin("joao").get("token").asText();
        long otherCompany = companyId("Multicore Moçambique Lda");

        call(get("/api/portal/summary").param("companyId", String.valueOf(otherCompany)), token, otherCompany)
                .andExpect(status().isForbidden());
    }

    @Test
    void pg07PortalSummaryRefusesSellerDesktopToken() throws Exception {
        long mz = companyId("Multicore Moçambique Lda");
        grantJoao(mz, "SELLER");
        String token = desktopLogin("joao");

        call(get("/api/portal/summary").param("companyId", String.valueOf(mz)), token, mz)
                .andExpect(status().isForbidden());
    }

    private void grantJoao(long companyId, String role) {
        jdbc.update("insert into app_user_companies (user_id, company_id, role)"
                + " select id, ?, ? from app_users where username = 'joao'", companyId, role);
    }

    private long companyId(String name) {
        return jdbc.queryForObject("select id from companies where name = ?", Long.class, name);
    }

    private ResultActions call(MockHttpServletRequestBuilder request, String token, long companyId) throws Exception {
        return mockMvc.perform(request.header("Authorization", "Bearer " + token)
                .header("X-Company-Id", String.valueOf(companyId)));
    }

    private String desktopLogin(String username) throws Exception {
        PortalTwoFactorFixture.disable(jdbc, username);
        return objectMapper.readTree(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"password\"}".formatted(username)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("token").asText();
    }

    private JsonNode portalLogin(String username) throws Exception {
        String secret = PortalTwoFactorFixture.enable(accountSecurityService, jdbc, username);
        String code = PortalTwoFactorFixture.currentCode(secret);
        return objectMapper.readTree(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"password","channel":"PORTAL","totpCode":"%s"}
                                """.formatted(username, code)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
}
