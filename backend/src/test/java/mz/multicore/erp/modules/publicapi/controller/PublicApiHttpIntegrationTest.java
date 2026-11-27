package mz.multicore.erp.modules.publicapi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Harness API-02 over HTTP (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.1, §3.1.1): API keys are listed, created and revoked
 * through the internal API of the active company, by the ADMIN only, and both changes are audited without the secret.
 * Seeded users: ana=ADMIN, joao=MANAGER, maria=EMPLOYEE (password "password").
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:public-api-http;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicApiHttpIntegrationTest {

    private static final String KEYS = "/api/integrations/api-keys";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;
    @Autowired private JdbcTemplate jdbc;

    private record Session(String token, String companyId) {}

    @AfterEach
    void cleanUp() {
        jdbc.update("delete from api_keys where name like 'API-02%'");
    }

    @Test
    void api02AdminCreatesListsAndRevokesKeysWithAudit() throws Exception {
        Session admin = login("ana", null);

        JsonNode created = read(send(post(KEYS), admin,
                "{\"name\":\"API-02 Loja online\",\"permissions\":[\"CATALOG_READ\",\"ORDERS_WRITE\"]}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String secret = created.get("secret").asText();
        assertThat(secret).startsWith("mk_live_");
        long id = created.get("key").get("id").asLong();
        String prefix = created.get("key").get("keyPrefix").asText();
        assertThat(created.get("key").get("active").asBoolean()).isTrue();

        String listed = mockMvc.perform(auth(get(KEYS), admin)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(read(listed).findValuesAsText("name")).contains("API-02 Loja online");
        assertThat(listed).doesNotContain(secret).doesNotContainIgnoringCase("keyHash");

        JsonNode revoked = read(send(post(KEYS + "/" + id + "/revoke"), admin, "")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(revoked.get("active").asBoolean()).isFalse();
        assertThat(revoked.get("revokedBy").asText()).isEqualTo("ana");
        send(post(KEYS + "/" + id + "/revoke"), admin, "").andExpect(status().isBadRequest());

        JsonNode logs = read(mockMvc.perform(auth(get("/api/audit/logs"), admin)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        boolean createLogged = false;
        boolean revokeLogged = false;
        for (JsonNode log : logs) {
            String details = log.get("details").asText();
            if (!details.contains("API-02 Loja online")) continue;
            assertThat(details).doesNotContain(secret).contains("mk_live_" + prefix);
            assertThat(log.get("username").asText()).isEqualTo("ana");
            createLogged |= "API_KEY_CREATE".equals(log.get("action").asText());
            revokeLogged |= "API_KEY_REVOKE".equals(log.get("action").asText());
        }
        assertThat(createLogged).isTrue();
        assertThat(revokeLogged).isTrue();
    }

    @Test
    void api02OnlyAdminManagesKeys() throws Exception {
        Session admin = login("ana", null);
        long id = read(send(post(KEYS), admin, "{\"name\":\"API-02 Contabilista\",\"permissions\":[\"ORDERS_READ\"]}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .get("key").get("id").asLong();

        for (String user : new String[]{"joao", "maria"}) {
            Session other = login(user, admin.companyId());
            mockMvc.perform(auth(get(KEYS), other)).andExpect(status().isForbidden());
            send(post(KEYS), other, "{\"name\":\"API-02 Intruso\",\"permissions\":[\"CATALOG_READ\"]}")
                    .andExpect(status().isForbidden());
            send(post(KEYS + "/" + id + "/revoke"), other, "").andExpect(status().isForbidden());
        }
        assertThat(jdbc.queryForObject("select count(*) from api_keys where name = 'API-02 Intruso'", Long.class))
                .isZero();
        assertThat(jdbc.queryForObject("select revoked_at is null from api_keys where id = ?", Boolean.class, id))
                .isTrue();
    }

    @Test
    void api02InvalidRequestsAndOtherCompanyKeysAreRejected() throws Exception {
        Session admin = login("ana", null);
        send(post(KEYS), admin, "{\"name\":\"  \",\"permissions\":[\"CATALOG_READ\"]}")
                .andExpect(status().isBadRequest());
        send(post(KEYS), admin, "{\"name\":\"API-02 Sem permissoes\",\"permissions\":[]}")
                .andExpect(status().isBadRequest());

        long id = read(send(post(KEYS), admin, "{\"name\":\"API-02 Outra empresa\",\"permissions\":[\"CATALOG_READ\"]}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .get("key").get("id").asLong();
        Session elsewhere = login("ana", otherCompanyThan(admin.companyId()));
        send(post(KEYS + "/" + id + "/revoke"), elsewhere, "").andExpect(status().isBadRequest());
        assertThat(read(mockMvc.perform(auth(get(KEYS), elsewhere)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).findValuesAsText("name"))
                .doesNotContain("API-02 Outra empresa");
    }

    // --- HTTP ---

    private String otherCompanyThan(String companyId) throws Exception {
        for (JsonNode company : loginJson("ana").get("companies")) {
            if (!company.get("id").asText().equals(companyId)) return company.get("id").asText();
        }
        throw new AssertionError("ana devia ter uma segunda empresa");
    }

    private Session login(String username, String companyId) throws Exception {
        JsonNode login = loginJson(username);
        String active = companyId != null ? companyId : login.get("companies").get(0).get("id").asText();
        return new Session(login.get("token").asText(), active);
    }

    private JsonNode loginJson(String username) throws Exception {
        return read(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"password\"}".formatted(username)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder req, Session s) {
        return req.header("Authorization", "Bearer " + s.token()).header("X-Company-Id", s.companyId());
    }

    private ResultActions send(MockHttpServletRequestBuilder req, Session s, String body) throws Exception {
        return mockMvc.perform(auth(req, s).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode read(String body) {
        try {
            return om.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException(body, e);
        }
    }
}
