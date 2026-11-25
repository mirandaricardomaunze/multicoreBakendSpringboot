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
 * PG-08 — aprovar/rejeitar pelo portal usa os endpoints e as regras do módulo de aprovações; rejeitar exige
 * motivo e o gestor não decide pedidos que pedem administrador (§3.4).
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:portal-http;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PortalHttpIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AccountSecurityService accountSecurityService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;

    private static final String PORTAL_DOC = "PORTAL_PG08";

    @AfterEach
    void removeExtraAccess() {
        jdbc.update("delete from app_user_companies where user_id = (select id from app_users where username = 'joao')"
                + " and company_id <> ?", companyId("Multicore Portugal Lda"));
        jdbc.update("delete from approval_history where approval_request_id in"
                + " (select id from approval_requests where document_type = ?)", PORTAL_DOC);
        jdbc.update("delete from approval_requests where document_type = ?", PORTAL_DOC);
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

    @Test
    void pg08PortalListsPendingWithTypeSubmitterAmountAndDate() throws Exception {
        JsonNode login = portalLogin("joao");
        long companyId = login.get("companies").get(0).get("id").asLong();
        long requestId = pendingRequest(companyId, "MANAGER");

        JsonNode pending = objectMapper.readTree(call(get("/api/approvals/pending"), login.get("token").asText(), companyId)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        JsonNode row = findById(pending, requestId);
        assertThat(row).isNotNull();
        assertThat(row.get("documentType").asText()).isEqualTo(PORTAL_DOC);
        assertThat(row.get("submitter").asText()).isEqualTo("maria");
        assertThat(row.get("amount").decimalValue()).isEqualByComparingTo("1460.00");
        assertThat(row.get("createdAt").isNull()).isFalse();
    }

    @Test
    void pg08PortalManagerApprovesManagerRequest() throws Exception {
        JsonNode login = portalLogin("joao");
        long companyId = login.get("companies").get(0).get("id").asLong();
        long requestId = pendingRequest(companyId, "MANAGER");

        call(post("/api/approvals/{id}/approve", requestId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"comments\":\"Aprovado no portal\"}"), login.get("token").asText(), companyId)
                .andExpect(status().isOk());

        assertThat(statusOf(requestId)).isEqualTo("APPROVED");
        assertThat(jdbc.queryForObject("select performed_by from approval_history where approval_request_id = ?",
                String.class, requestId)).isEqualTo("joao");
    }

    @Test
    void pg08PortalRejectWithoutReasonIsRefused() throws Exception {
        JsonNode login = portalLogin("joao");
        long companyId = login.get("companies").get(0).get("id").asLong();
        long requestId = pendingRequest(companyId, "MANAGER");

        String body = call(post("/api/approvals/{id}/reject", requestId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"comments\":\"  \"}"), login.get("token").asText(), companyId)
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();

        assertThat(body).contains("É obrigatório indicar um motivo para a rejeição.");
        assertThat(statusOf(requestId)).isEqualTo("PENDING");
    }

    @Test
    void pg08PortalRejectWithReasonRecordsIt() throws Exception {
        JsonNode login = portalLogin("joao");
        long companyId = login.get("companies").get(0).get("id").asLong();
        long requestId = pendingRequest(companyId, "MANAGER");

        call(post("/api/approvals/{id}/reject", requestId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"comments\":\"Desconto acima do combinado\"}"), login.get("token").asText(), companyId)
                .andExpect(status().isOk());

        assertThat(statusOf(requestId)).isEqualTo("REJECTED");
        assertThat(jdbc.queryForObject("select rejection_reason from approval_requests where id = ?",
                String.class, requestId)).isEqualTo("Desconto acima do combinado");
    }

    @Test
    void pg08PortalManagerCannotDecideAdminRequest() throws Exception {
        JsonNode login = portalLogin("joao");
        long companyId = login.get("companies").get(0).get("id").asLong();
        long requestId = pendingRequest(companyId, "ADMIN");
        String token = login.get("token").asText();

        call(post("/api/approvals/{id}/approve", requestId), token, companyId).andExpect(status().isBadRequest());
        call(post("/api/approvals/{id}/reject", requestId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"comments\":\"Não\"}"), token, companyId).andExpect(status().isBadRequest());

        assertThat(statusOf(requestId)).isEqualTo("PENDING");
    }

    @Test
    void pg08PortalCannotDecideOtherCompanyRequest() throws Exception {
        JsonNode login = portalLogin("joao");
        long own = login.get("companies").get(0).get("id").asLong();
        long otherRequest = pendingRequest(companyId("Multicore Moçambique Lda"), "MANAGER");

        call(post("/api/approvals/{id}/approve", otherRequest), login.get("token").asText(), own)
                .andExpect(status().isBadRequest());

        assertThat(statusOf(otherRequest)).isEqualTo("PENDING");
    }

    private long pendingRequest(long companyId, String requiredRole) {
        jdbc.update("insert into approval_requests (company_id, document_type, document_id, amount, submitter, status,"
                + " required_role, description, created_at) values (?, ?, ?, 1460.00, 'maria', 'PENDING', ?,"
                + " 'Pedido de teste do portal', current_timestamp)", companyId, PORTAL_DOC, System.nanoTime(), requiredRole);
        return jdbc.queryForObject("select max(id) from approval_requests where document_type = ?", Long.class, PORTAL_DOC);
    }

    private String statusOf(long requestId) {
        return jdbc.queryForObject("select status from approval_requests where id = ?", String.class, requestId);
    }

    private static JsonNode findById(JsonNode rows, long id) {
        for (JsonNode row : rows) {
            if (row.get("id").asLong() == id) {
                return row;
            }
        }
        return null;
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
